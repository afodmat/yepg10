package com.yep.platform.auth;

import com.yep.platform.auth.AuthDtos.*;
import com.yep.platform.common.ApiException;
import com.yep.platform.security.CookieService;
import com.yep.platform.security.JwtService;
import com.yep.platform.user.UserDtos.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final JwtService jwt;
    private final CookieService cookies;

    public AuthController(AuthService auth, JwtService jwt, CookieService cookies) { this.auth = auth; this.jwt = jwt; this.cookies = cookies; }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest r, HttpServletRequest req) {
        try { return auth.register(r, req.getRemoteAddr()); }
        catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Simultaneous signups with the same email: answer like a normal signup so existence is never revealed.
            return new MessageResponse(AuthService.REGISTER_MESSAGE);
        }
    }

    @GetMapping("/activate")
    public MessageResponse activate(@RequestParam String token, HttpServletRequest req) {
        auth.activate(token, req.getRemoteAddr());
        return new MessageResponse("Your account is active. You can now log in.");
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest req, HttpServletResponse res) {
        return start(auth.login(r, req.getRemoteAddr()), res);
    }

    @GetMapping("/csrf")
    public void csrf() {}

    @PostMapping("/refresh")
    public UserResponse refresh(HttpServletRequest req, HttpServletResponse res) {
        String raw = cookies.read(req, CookieService.REFRESH).orElseThrow(() -> ApiException.unauthorized("Session expired. Please log in again."));
        return start(auth.refresh(raw), res);
    }

    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest req, HttpServletResponse res) {
        auth.logout(cookies.read(req, CookieService.REFRESH).orElse(null));
        cookies.clear(res);
    }

    @PostMapping("/forgot-password") @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse forgot(@Valid @RequestBody ForgotPasswordRequest r, HttpServletRequest req) { return auth.forgotPassword(r, req.getRemoteAddr()); }

    @PostMapping("/reset-password")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest r, HttpServletRequest req) {
        auth.resetPassword(r, req.getRemoteAddr());
        return new MessageResponse("Password updated. You can now log in.");
    }

    private UserResponse start(AuthService.Session s, HttpServletResponse res) {
        cookies.writeAccess(res, jwt.createAccessToken(s.user()));
        cookies.writeRefresh(res, s.refreshToken());
        return UserResponse.of(s.user(), s.profile());
    }
}
