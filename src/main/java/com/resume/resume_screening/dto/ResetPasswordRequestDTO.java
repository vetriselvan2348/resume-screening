package com.resume.resume_screening.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @NotBlank(message = "OTP is required")
    @Pattern(
            regexp = "\\d{6}",
            message = "OTP must contain exactly 6 digits"
    )
    private String otp;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must contain at least 6 characters")
    private String newPassword;
}
