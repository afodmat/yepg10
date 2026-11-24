package com.yep.platform.user;

import com.yep.platform.audit.AuditService;
import com.yep.platform.common.ApiException;
import com.yep.platform.user.UserDtos.*;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final com.yep.platform.auth.SessionRevoker revoker;

    public UserService(UserRepository users, ProfileRepository profiles, PasswordEncoder encoder, AuditService audit, com.yep.platform.auth.SessionRevoker revoker) {
        this.users = users; this.profiles = profiles; this.encoder = encoder; this.audit = audit; this.revoker = revoker;
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        var u = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Please log in to continue."));
        if (u.getStatus() != UserStatus.ACTIVE) throw ApiException.unauthorized("Please log in to continue.");
        return UserResponse.of(u, profiles.findById(userId).orElseThrow());
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile(Long userId) {
        return ProfileResponse.of(users.findById(userId).orElseThrow(), profiles.findById(userId).orElseThrow());
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest r) {
        var u = users.findById(userId).orElseThrow();
        var p = profiles.findById(userId).orElseThrow();
        p.setFirstName(r.firstName().trim()); p.setLastName(r.lastName().trim());
        p.setPhone(r.phone() == null || r.phone().isBlank() ? null : r.phone().trim());
        p.setCountry(r.country().trim()); p.setAge(r.age());
        p.setBio(r.bio()); p.setSkills(r.skills()); p.setInterests(r.interests());
        p.setEducation(r.education()); p.setOrganization(r.organization());
        return ProfileResponse.of(u, p);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest r, String ip) {
        var u = users.findById(userId).orElseThrow();
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash())) throw ApiException.badRequest("Your current password is incorrect.");
        u.setPasswordHash(encoder.encode(r.newPassword()));
        revoker.revokeAll(userId);   // sign out other devices
        audit.record(u, u.getEmail(), "PASSWORD_CHANGED", "User", String.valueOf(userId), Map.of(), ip);
    }
}
