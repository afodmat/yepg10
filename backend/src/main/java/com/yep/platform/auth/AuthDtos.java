package com.yep.platform.auth;

import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}
    private static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).+$";

    /** No role field: self-registration always creates a PARTICIPANT. */
    public record RegisterRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 254) String email,
            @Pattern(regexp = "^\\+?[0-9 ()-]{7,30}$", message = "Enter a valid phone number") String phone,
            @NotBlank @Size(max = 60) String country,
            @NotNull @Min(value = 18, message = "YEP is for ages 18–35") @Max(value = 35, message = "YEP is for ages 18–35") Integer age,
            @NotBlank @Size(min = 8, max = 72, message = "Password must be 8–72 characters") @Pattern(regexp = PASSWORD_RULE, message = "Use letters and at least one number") String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record ResetPasswordRequest(@NotBlank String token,
            @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = PASSWORD_RULE, message = "Use letters and at least one number") String newPassword) {}
    public record MessageResponse(String message) {}
}
