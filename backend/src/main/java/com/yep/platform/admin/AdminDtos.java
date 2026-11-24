package com.yep.platform.admin;

import com.yep.platform.user.Role;
import com.yep.platform.user.UserStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

public final class AdminDtos {
    private AdminDtos() {}
    public record UserRow(Long id, String email, String firstName, String lastName, Role role, UserStatus status, String country, Instant createdAt, Instant lastLoginAt) {}
    public record CreateUserRequest(@NotBlank @Size(max = 80) String firstName, @NotBlank @Size(max = 80) String lastName, @NotBlank @Email String email,
                                    @NotBlank @Size(max = 60) String country, @NotNull Role role,
                                    @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Use letters and at least one number") String temporaryPassword) {}
    public record RoleRequest(@NotNull Role role) {}
    public record StatusRequest(@NotNull UserStatus status) {}
    public record AuditRow(Long id, String actorEmail, String action, String entityType, String entityId, Map<String, Object> metadata, Instant createdAt) {}
    public record BroadcastRequest(@NotBlank @Size(max = 160) String title, @Size(max = 500) String body, Role role) {}
    public record AchievementRequest(@NotBlank @Size(max = 40) String code, @NotBlank @Size(max = 80) String name, @NotBlank @Size(max = 300) String description,
                                     @NotBlank @Size(max = 20) String icon, @NotBlank @Size(max = 300) String criteria, boolean active) {}
}
