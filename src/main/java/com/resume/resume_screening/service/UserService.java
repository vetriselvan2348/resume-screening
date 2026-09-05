package com.resume.resume_screening.service;

import com.resume.resume_screening.dto.LoginRequestDTO;
import com.resume.resume_screening.dto.LoginResponseDTO;
import com.resume.resume_screening.dto.ProfileUpdateDTO;
import com.resume.resume_screening.dto.RecruiterRegisterRequestDTO;
import com.resume.resume_screening.dto.UserRequestDTO;
import com.resume.resume_screening.dto.UserResponseDTO;
import com.resume.resume_screening.exception.DuplicateResourceException;
import com.resume.resume_screening.exception.ResourceNotFoundException;
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

    private static final String RECRUITER_ADMIN_PASSWORD = "admin@1234";

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponseDTO saveUser(UserRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        User savedUser = userRepository.save(user);
        return new UserResponseDTO(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    }

    public UserResponseDTO registerRecruiter(RecruiterRegisterRequestDTO request) {
        if (!RECRUITER_ADMIN_PASSWORD.equals(request.getAdminPassword())) {
            throw new IllegalArgumentException("Invalid admin password");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.RECRUITER);
        User savedUser = userRepository.save(user);
        return new UserResponseDTO(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    }

    public UserResponseDTO getMyProfile() {
        User user = getLoggedInUser();
        return new UserResponseDTO(user.getId(), user.getName(), user.getEmail());
    }

    public UserResponseDTO updateMyProfile(ProfileUpdateDTO request) {
        User user = getLoggedInUser();

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        User updatedUser = userRepository.save(user);
        return new UserResponseDTO(updatedUser.getId(), updatedUser.getName(), updatedUser.getEmail());
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserResponseDTO(user.getId(), user.getName(), user.getEmail());
    }

    public UserResponseDTO updateUser(Long id, UserRequestDTO userRequestDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.getEmail().equalsIgnoreCase(userRequestDTO.getEmail()) &&
                userRepository.existsByEmail(userRequestDTO.getEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }
        user.setName(userRequestDTO.getName());
        user.setEmail(userRequestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        user.setRole(userRequestDTO.getRole());
        User updatedUser = userRepository.save(user);
        return new UserResponseDTO(updatedUser.getId(), updatedUser.getName(), updatedUser.getEmail());
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.delete(user);
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new LoginResponseDTO(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
