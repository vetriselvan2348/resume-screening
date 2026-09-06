package com.resume.resume_screening.controller;

import com.resume.resume_screening.dto.EmailRequestDTO;
import com.resume.resume_screening.dto.LoginRequestDTO;
import com.resume.resume_screening.dto.OtpRequestDTO;
import com.resume.resume_screening.dto.ResetPasswordRequestDTO;
import com.resume.resume_screening.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = "http://localhost:5173",
        allowCredentials = "true"
)
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestDTO request) {

        return ResponseEntity.ok(
                userService.login(request)
        );
    }

    @PostMapping("/verify-registration")
    public ResponseEntity<?> verifyRegistration(
            @Valid @RequestBody OtpRequestDTO request) {

        userService.verifyRegistrationOtp(
                request.getEmail(),
                request.getOtp()
        );

        return ResponseEntity.ok(
                "Email verified successfully"
        );
    }

    @PostMapping("/resend-registration-otp")
    public ResponseEntity<?> resendRegistrationOtp(
            @Valid @RequestBody EmailRequestDTO request) {

        userService.resendRegistrationOtp(
                request.getEmail()
        );

        return ResponseEntity.ok(
                "OTP sent successfully"
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody EmailRequestDTO request) {

        userService.forgotPassword(request.getEmail());

        return ResponseEntity.ok(
                "If an account exists with this email, a password reset OTP has been sent"
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {

        userService.resetPassword(request);

        return ResponseEntity.ok(
                "Password reset successfully"
        );
    }
}
