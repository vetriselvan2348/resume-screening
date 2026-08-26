package com.resume.resume_screening.service;

import com.resume.resume_screening.dto.LoginRequestDTO;
import com.resume.resume_screening.dto.LoginResponseDTO;
import com.resume.resume_screening.dto.UserRequestDTO;
import com.resume.resume_screening.dto.UserResponseDTO;
import com.resume.resume_screening.model.Role;
import com.resume.resume_screening.model.User;
import com.resume.resume_screening.repository.UserRepository;
import com.resume.resume_screening.security.JwtService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;


    @Test
    void saveUser_shouldCreateUserSuccessfully() {

        UserRequestDTO request = new UserRequestDTO(
                "Test User",
                "test@test.com",
                "Test@123",
                Role.CANDIDATE
        );

        when(passwordEncoder.encode("Test@123"))
                .thenReturn("hashedPassword");

        User savedUser = new User();

        savedUser.setId(1L);
        savedUser.setName("Test User");
        savedUser.setEmail("test@test.com");
        savedUser.setPassword("hashedPassword");
        savedUser.setRole(Role.CANDIDATE);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        UserResponseDTO response =
                userService.saveUser(request);

        assertEquals(1L, response.getId());
        assertEquals("Test User", response.getName());
        assertEquals("test@test.com", response.getEmail());

        verify(passwordEncoder)
                .encode("Test@123");

        verify(userRepository)
                .save(any(User.class));
    }


    @Test
    void getUserById_shouldReturnUser() {

        User user = new User();

        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@test.com");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserResponseDTO response =
                userService.getUserById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Test User", response.getName());
        assertEquals("test@test.com", response.getEmail());

        verify(userRepository)
                .findById(1L);
    }


    @Test
    void login_shouldReturnJwtToken() {

        User user = new User();

        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@test.com");
        user.setPassword("hashedPassword");
        user.setRole(Role.CANDIDATE);

        LoginRequestDTO request =
                new LoginRequestDTO(
                        "test@test.com",
                        "Test@123"
                );

        when(userRepository.findByEmail(
                "test@test.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Test@123",
                "hashedPassword"))
                .thenReturn(true);

        when(jwtService.generateToken(
                "test@test.com",
                "CANDIDATE"))
                .thenReturn("jwt-token");

        LoginResponseDTO response =
                userService.login(request);

        assertEquals(
                "jwt-token",
                response.getToken()
        );

        assertEquals(
                1L,
                response.getUserId()
        );

        assertEquals(
                "Test User",
                response.getName()
        );

        assertEquals(
                "test@test.com",
                response.getEmail()
        );

        verify(jwtService)
                .generateToken(
                        "test@test.com",
                        "CANDIDATE"
                );
    }


    @Test
    void login_shouldFailWithWrongPassword() {

        User user = new User();

        user.setEmail("test@test.com");
        user.setPassword("hashedPassword");
        user.setRole(Role.CANDIDATE);

        LoginRequestDTO request =
                new LoginRequestDTO(
                        "test@test.com",
                        "WrongPassword"
                );

        when(userRepository.findByEmail(
                "test@test.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword",
                "hashedPassword"))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.login(request)
        );

        verify(jwtService, never())
                .generateToken(anyString(), anyString());
    }
}