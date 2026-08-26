package com.resume.resume_screening.service;

import com.resume.resume_screening.dto.LoginRequestDTO;
import com.resume.resume_screening.dto.LoginResponseDTO;
import com.resume.resume_screening.dto.UserRequestDTO;
import com.resume.resume_screening.dto.UserResponseDTO;
import com.resume.resume_screening.exception.ResourceNotFoundException;
import com.resume.resume_screening.model.User;
import com.resume.resume_screening.repository.UserRepository;
import com.resume.resume_screening.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponseDTO saveUser(UserRequestDTO request) {

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);

        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );
    }

    public UserResponseDTO getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

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
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        user.setName(userRequestDTO.getName());
        user.setEmail(userRequestDTO.getEmail());

        // Hash password before updating
        user.setPassword(
                passwordEncoder.encode(
                        userRequestDTO.getPassword()
                )
        );

        user.setRole(userRequestDTO.getRole());

        User updatedUser =
                userRepository.save(user);

        return new UserResponseDTO(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail()
        );
    }

    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        userRepository.delete(user);
    }

    public LoginResponseDTO login(
            LoginRequestDTO request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

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
                user.getEmail()
        );
    }
}