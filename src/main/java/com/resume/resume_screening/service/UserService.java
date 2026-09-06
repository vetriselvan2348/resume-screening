package com.resume.resume_screening.service;

import com.resume.resume_screening.dto.LoginRequestDTO;
import com.resume.resume_screening.dto.LoginResponseDTO;
import com.resume.resume_screening.dto.ProfileUpdateDTO;
import com.resume.resume_screening.dto.RecruiterRegisterRequestDTO;
import com.resume.resume_screening.dto.ResetPasswordRequestDTO;
import com.resume.resume_screening.dto.UserRequestDTO;
import com.resume.resume_screening.dto.UserResponseDTO;
import com.resume.resume_screening.exception.DuplicateResourceException;
import com.resume.resume_screening.exception.ResourceNotFoundException;
import com.resume.resume_screening.model.OtpPurpose;
import com.resume.resume_screening.model.Role;
import com.resume.resume_screening.model.User;
import com.resume.resume_screening.repository.UserRepository;
import com.resume.resume_screening.security.JwtService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    private static final String RECRUITER_ADMIN_PASSWORD = "admin@1234";

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
    }

    public UserResponseDTO saveUser(UserRequestDTO request) {

        User existingUser = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (existingUser != null) {
            if (Boolean.FALSE.equals(existingUser.getEmailVerified())) {
                existingUser.setName(request.getName());
                existingUser.setPassword(
                        passwordEncoder.encode(request.getPassword())
                );
                existingUser.setRole(request.getRole());

                User updatedUser = userRepository.save(existingUser);

                otpService.generateAndSendOtp(
                        updatedUser.getEmail(),
                        OtpPurpose.REGISTRATION
                );

                return new UserResponseDTO(
                        updatedUser.getId(),
                        updatedUser.getName(),
                        updatedUser.getEmail()
                );
            }

            throw new DuplicateResourceException(
                    "Email is already registered"
            );
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        otpService.generateAndSendOtp(
                savedUser.getEmail(),
                OtpPurpose.REGISTRATION
        );

        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );
    }

    public UserResponseDTO registerRecruiter(
            RecruiterRegisterRequestDTO request) {

        if (!RECRUITER_ADMIN_PASSWORD.equals(request.getAdminPassword())) {
            throw new IllegalArgumentException("Invalid admin password");
        }

        User existingUser = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (existingUser != null) {
            if (Boolean.FALSE.equals(existingUser.getEmailVerified())) {
                existingUser.setName(request.getName());
                existingUser.setPassword(
                        passwordEncoder.encode(request.getPassword())
                );
                existingUser.setRole(Role.RECRUITER);

                User updatedUser = userRepository.save(existingUser);

                otpService.generateAndSendOtp(
                        updatedUser.getEmail(),
                        OtpPurpose.REGISTRATION
                );

                return new UserResponseDTO(
                        updatedUser.getId(),
                        updatedUser.getName(),
                        updatedUser.getEmail()
                );
            }

            throw new DuplicateResourceException(
                    "Email is already registered"
            );
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.RECRUITER);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        otpService.generateAndSendOtp(
                savedUser.getEmail(),
                OtpPurpose.REGISTRATION
        );

        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );
    }

    public void verifyRegistrationOtp(String email, String otp) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException(
                    "Email is already verified"
            );
        }

        otpService.verifyOtp(
                email,
                otp,
                OtpPurpose.REGISTRATION
        );

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    public void resendRegistrationOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException(
                    "Email is already verified"
            );
        }

        otpService.generateAndSendOtp(
                email,
                OtpPurpose.REGISTRATION
        );
    }

    public void forgotPassword(String email) {

        userRepository.findByEmail(email).ifPresent(user ->
                otpService.generateAndSendOtp(
                        user.getEmail(),
                        OtpPurpose.PASSWORD_RESET
                )
        );
    }

    public void resetPassword(ResetPasswordRequestDTO request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or OTP"
                        ));

        otpService.verifyOtp(
                request.getEmail(),
                request.getOtp(),
                OtpPurpose.PASSWORD_RESET
        );

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(user);
    }

    public UserResponseDTO getMyProfile() {
        User user = getLoggedInUser();

        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    public UserResponseDTO updateMyProfile(
            ProfileUpdateDTO request) {

        User user = getLoggedInUser();

        if (request.getName() != null &&
                !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        if (request.getPassword() != null &&
                !request.getPassword().trim().isEmpty()) {
            user.setPassword(
                    passwordEncoder.encode(request.getPassword().trim())
            );
        }

        User updatedUser = userRepository.save(user);

        return new UserResponseDTO(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail()
        );
    }

    public UserResponseDTO getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    public UserResponseDTO updateUser(
            Long id,
            UserRequestDTO userRequestDTO) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equalsIgnoreCase(
                userRequestDTO.getEmail()) &&
                userRepository.existsByEmail(
                        userRequestDTO.getEmail())) {

            throw new DuplicateResourceException(
                    "Email is already registered"
            );
        }

        user.setName(userRequestDTO.getName());
        user.setEmail(userRequestDTO.getEmail());
        user.setPassword(
                passwordEncoder.encode(
                        userRequestDTO.getPassword()
                )
        );
        user.setRole(userRequestDTO.getRole());

        User updatedUser = userRepository.save(user);

        return new UserResponseDTO(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail()
        );
    }

    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        userRepository.delete(user);
    }

    public LoginResponseDTO login(LoginRequestDTO request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException(
                    "Please verify your email before logging in"
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new LoginResponseDTO(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    private User getLoggedInUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }
}
