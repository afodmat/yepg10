package com.yep.platform.user;

import jakarta.validation.constraints.*;

public final class UserDtos {
    private UserDtos() {}

    public record UserResponse(Long id, String email, String firstName, String lastName, Role role, String country, UserStatus status, Long photoFileId) {
        public static UserResponse of(User u, Profile p) {
            return new UserResponse(u.getId(), u.getEmail(), p.getFirstName(), p.getLastName(), u.getRole(), p.getCountry(), u.getStatus(), p.getPhotoFileId());
        }
    }

    public record ProfileResponse(String firstName, String lastName, String email, String phone, String country, Short age,
                                  String bio, String skills, String interests, String education, String organization,
                                  Role role, UserStatus status, Long photoFileId) {
        public static ProfileResponse of(User u, Profile p) {
            return new ProfileResponse(p.getFirstName(), p.getLastName(), u.getEmail(), p.getPhone(), p.getCountry(), p.getAge(),
                    p.getBio(), p.getSkills(), p.getInterests(), p.getEducation(), p.getOrganization(), u.getRole(), u.getStatus(), p.getPhotoFileId());
        }
    }

    /** Deliberately has no role/status/email fields: privileged data can never be set from here. */
    public record UpdateProfileRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @Pattern(regexp = "^\\+?[0-9 ()-]{7,30}$", message = "Enter a valid phone number") String phone,
            @NotBlank @Size(max = 60) String country,
            @Min(16) @Max(120) Short age,
            @Size(max = 1000) String bio, @Size(max = 500) String skills, @Size(max = 500) String interests,
            @Size(max = 200) String education, @Size(max = 200) String organization) {}

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Use letters and at least one number") String newPassword) {}
}
