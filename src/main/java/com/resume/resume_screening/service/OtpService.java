package com.resume.resume_screening.service;

import com.resume.resume_screening.model.OtpPurpose;
import com.resume.resume_screening.model.OtpVerification;
import com.resume.resume_screening.repository.OtpVerificationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    public OtpService(
            OtpVerificationRepository otpRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public void generateAndSendOtp(String email, OtpPurpose purpose) {

        LocalDateTime now = LocalDateTime.now();

        OtpVerification existing =
                otpRepository
                        .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                                email,
                                purpose
                        )
                        .orElse(null);

        if (existing != null) {
            LocalDateTime cooldownEnd =
                    existing.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS);

            if (now.isBefore(cooldownEnd)) {
                long remainingSeconds =
                        java.time.Duration.between(now, cooldownEnd).getSeconds();

                throw new IllegalArgumentException(
                        "Please wait " + remainingSeconds + " seconds before requesting another OTP"
                );
            }

            existing.setUsed(true);
            otpRepository.save(existing);
        }

        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        OtpVerification verification = new OtpVerification();

        verification.setEmail(email);
        verification.setOtpHash(passwordEncoder.encode(otp));
        verification.setPurpose(purpose);
        verification.setExpiresAt(
                now.plusMinutes(OTP_EXPIRY_MINUTES)
        );
        verification.setAttempts(0);
        verification.setUsed(false);
        verification.setCreatedAt(now);

        otpRepository.save(verification);

        String purposeText =
                purpose == OtpPurpose.REGISTRATION
                        ? "registration"
                        : "password reset";

        emailService.sendOtpEmail(
                email,
                otp,
                purposeText
        );
    }

    public void verifyOtp(
            String email,
            String otp,
            OtpPurpose purpose) {

        OtpVerification verification =
                otpRepository
                        .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                                email,
                                purpose
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid or expired OTP"
                                ));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            verification.setUsed(true);
            otpRepository.save(verification);

            throw new IllegalArgumentException(
                    "OTP has expired"
            );
        }

        if (verification.getAttempts() >= MAX_ATTEMPTS) {
            verification.setUsed(true);
            otpRepository.save(verification);

            throw new IllegalArgumentException(
                    "Too many OTP attempts"
            );
        }

        if (!passwordEncoder.matches(
                otp,
                verification.getOtpHash())) {

            verification.setAttempts(
                    verification.getAttempts() + 1
            );

            otpRepository.save(verification);

            throw new IllegalArgumentException(
                    "Invalid OTP"
            );
        }

        verification.setUsed(true);
        otpRepository.save(verification);
    }
}
