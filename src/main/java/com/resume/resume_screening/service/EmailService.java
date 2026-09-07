package com.resume.resume_screening.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String email, String otp, String purpose) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Resume Screening System - OTP Verification");
        message.setText(
                "Your OTP for " + purpose + " is: " + otp +
                "\n\nThis OTP is valid for 5 minutes." +
                "\n\nDo not share this OTP with anyone."
        );

        mailSender.send(message);
    }
}
