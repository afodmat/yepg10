package com.yep.platform.auth;

import com.yep.platform.audit.AuditService;
import com.yep.platform.auth.AuthDtos.*;
import com.yep.platform.common.ApiException;
import com.yep.platform.mail.MailService;
import com.yep.platform.security.JwtProperties;
import com.yep.platform.user.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final int MAX_FAILED = 5;
    private static final Duration LOCK = Duration.ofMinutes(15);
    private static final String GENERIC_LOGIN_ERROR = "Invalid email or password.";
    static final String REGISTER_MESSAGE = "Almost there! Check your email for a link to activate your account.";

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final RefreshTokenRepository refreshTokens;
    private final OneTimeTokenRepository oneTime;
    private final PasswordEncoder encoder;
    private final MailService mail;
    private final AuditService audit;
    private final JwtProperties jwt;
    private final String apiUrl, frontendUrl;
    private final String dummyHash;

    public AuthService(UserRepository users, ProfileRepository profiles, RefreshTokenRepository refreshTokens,
            OneTimeTokenRepository oneTime,
            PasswordEncoder encoder, MailService mail, AuditService audit, JwtProperties jwt,
            @Value("${yep.api-url}") String apiUrl, @Value("${yep.frontend-url}") String frontendUrl) {
        this.users = users;
        this.profiles = profiles;
        this.refreshTokens = refreshTokens;
        this.oneTime = oneTime;
        this.encoder = encoder;
        this.mail = mail;
        this.audit = audit;
        this.jwt = jwt;
        this.apiUrl = apiUrl;
        this.frontendUrl = frontendUrl;
        this.dummyHash = encoder.encode("not-a-real-password"); // equalises timing for unknown emails
    }

    // ---------- registration & activation ----------
    @Transactional
    public MessageResponse register(RegisterRequest r, String ip) {
        String email = r.email().trim().toLowerCase();
        if (users.findByEmailIgnoreCase(email).isPresent())
            return new MessageResponse(REGISTER_MESSAGE); // do not reveal which emails exist
        var user = users.save(new User(email, encoder.encode(r.password()), Role.PARTICIPANT, UserStatus.PENDING));
        profiles.save(new Profile(user, r.firstName().trim(), r.lastName().trim(), blankToNull(r.phone()),
                r.country().trim(), r.age().shortValue()));
        String token = issueOneTime(user, OneTimeToken.Type.ACTIVATION, Duration.ofHours(48));
        mail.send(email, "Activate your YEP account",
                "Welcome to YEP, " + r.firstName() + "!\n\nActivate your account:\n" + frontendUrl + "/#activate?token="
                        + token + "\n\nThis link expires in 48 hours.");
        audit.record(user, email, "USER_REGISTERED", "User", String.valueOf(user.getId()),
                Map.of("role", "PARTICIPANT"), ip);
        return new MessageResponse(REGISTER_MESSAGE);
    }

    @Transactional
    public void activate(String rawToken, String ip) {
        var user = consume(rawToken, OneTimeToken.Type.ACTIVATION);
        if (user.getStatus() == UserStatus.PENDING)
            user.setStatus(UserStatus.ACTIVE);
        audit.record(user, user.getEmail(), "USER_ACTIVATED", "User", String.valueOf(user.getId()), null, ip);
    }

    // ---------- login / refresh / logout ----------
    public record Session(User user, Profile profile, String refreshToken) {
    }

    /** noRollbackFor: failed-attempt counters must persist even though we throw. */
    @Transactional(noRollbackFor = ApiException.class)
    public Session login(LoginRequest r, String ip) {
        String email = r.email().trim().toLowerCase();
        var found = users.findByEmailIgnoreCase(email);
        if (found.isEmpty()) {
            encoder.matches(r.password(), dummyHash);
            audit.record(null, email, "LOGIN_FAILED", "User", null, Map.of("reason", "unknown_email"), ip);
            throw ApiException.unauthorized(GENERIC_LOGIN_ERROR);
        }
        var user = found.get();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now()))
            throw ApiException.tooMany("Too many failed attempts. Try again in a few minutes or reset your password.");
        if (!encoder.matches(r.password(), user.getPasswordHash())) {
            int n = user.getFailedAttempts() + 1;
            user.setFailedAttempts(n);
            if (n >= MAX_FAILED) {
                user.setLockedUntil(Instant.now().plus(LOCK));
                user.setFailedAttempts(0);
            }
            audit.record(user, email, "LOGIN_FAILED", "User", String.valueOf(user.getId()),
                    Map.of("reason", "bad_password"), ip);
            throw ApiException.unauthorized(GENERIC_LOGIN_ERROR);
        }
        if (user.getStatus() == UserStatus.PENDING)
            throw ApiException.forbidden("Please activate your account using the link we emailed you.");
        if (user.getStatus() == UserStatus.DISABLED)
            throw ApiException.forbidden("This account has been disabled. Contact YEP support.");
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        audit.record(user, email, user.getRole() == Role.ADMIN ? "ADMIN_LOGIN" : "LOGIN_SUCCESS", "User",
                String.valueOf(user.getId()), null, ip);
        return new Session(user, profileOf(user), issueRefresh(user));
    }

    /**
     * Rotating refresh tokens; re-use of an already-rotated token revokes every
     * session of that user.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Session refresh(String raw) {
        var t = refreshTokens.findByTokenHash(TokenUtil.hash(raw))
                .orElseThrow(() -> ApiException.unauthorized("Session expired. Please log in again."));
        if (t.getRevokedAt() != null) {
            refreshTokens.revokeAllForUser(t.getUserId());
            throw ApiException.unauthorized("Session expired. Please log in again.");
        }
        if (t.getExpiresAt().isBefore(Instant.now()))
            throw ApiException.unauthorized("Session expired. Please log in again.");
        var user = users.findById(t.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("Session expired. Please log in again."));
        if (user.getStatus() != UserStatus.ACTIVE)
            throw ApiException.unauthorized("Session expired. Please log in again.");
        t.revoke();
        return new Session(user, profileOf(user), issueRefresh(user));
    }

    @Transactional
    public void logout(String rawRefresh) {
        if (rawRefresh == null)
            return;
        refreshTokens.findByTokenHash(TokenUtil.hash(rawRefresh)).ifPresent(RefreshToken::revoke);
    }

    // ---------- password reset ----------
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest r, String ip) {
        users.findByEmailIgnoreCase(r.email().trim()).filter(u -> u.getStatus() != UserStatus.DISABLED).ifPresent(u -> {
            String token = issueOneTime(u, OneTimeToken.Type.PASSWORD_RESET, Duration.ofHours(1));
            mail.send(u.getEmail(), "Reset your YEP password",
                    "Reset your password:\n" + frontendUrl + "/#reset-password?token=" + token
                            + "\n\nThis link expires in 1 hour. If you did not ask for it, ignore this email.");
            audit.record(u, u.getEmail(), "PASSWORD_RESET_REQUESTED", "User", String.valueOf(u.getId()), null, ip);
        });
        return new MessageResponse("If that email is registered, a reset link is on its way.");
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest r, String ip) {
        var user = consume(r.token(), OneTimeToken.Type.PASSWORD_RESET);
        user.setPasswordHash(encoder.encode(r.newPassword()));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.PENDING)
            user.setStatus(UserStatus.ACTIVE); // reset link proves email ownership
        refreshTokens.revokeAllForUser(user.getId());
        audit.record(user, user.getEmail(), "PASSWORD_RESET", "User", String.valueOf(user.getId()), null, ip);
    }

    // ---------- helpers ----------
    private Profile profileOf(User u) {
        return profiles.findById(u.getId()).orElseThrow();
    }

    private String issueRefresh(User u) {
        String raw = TokenUtil.newToken();
        refreshTokens.save(new RefreshToken(u.getId(), TokenUtil.hash(raw), Instant.now().plus(jwt.refreshTtl())));
        return raw;
    }

    private String issueOneTime(User u, OneTimeToken.Type type, Duration ttl) {
        String raw = TokenUtil.newToken();
        oneTime.save(new OneTimeToken(u.getId(), type, TokenUtil.hash(raw), Instant.now().plus(ttl)));
        return raw;
    }

    private User consume(String raw, OneTimeToken.Type type) {
        var t = oneTime.findByTokenHash(TokenUtil.hash(raw)).filter(x -> x.getType() == type)
                .orElseThrow(() -> ApiException.badRequest("This link is invalid or has expired."));
        if (t.getUsedAt() != null || t.getExpiresAt().isBefore(Instant.now()))
            throw ApiException.badRequest("This link is invalid or has expired.");
        t.markUsed();
        return users.findById(t.getUserId())
                .orElseThrow(() -> ApiException.badRequest("This link is invalid or has expired."));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
