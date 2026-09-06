package com.resume.resume_screening.controller;

import com.resume.resume_screening.dto.ProfileUpdateDTO;
import com.resume.resume_screening.dto.RecruiterRegisterRequestDTO;
import com.resume.resume_screening.dto.UserRequestDTO;
import com.resume.resume_screening.dto.UserResponseDTO;
import com.resume.resume_screening.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody UserRequestDTO userRequestDTO) {

        UserResponseDTO response =
                userService.saveUser(userRequestDTO);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/recruiter/register")
    public ResponseEntity<UserResponseDTO> registerRecruiter(
            @Valid @RequestBody RecruiterRegisterRequestDTO request) {

        UserResponseDTO response =
                userService.registerRecruiter(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyProfile() {

        return ResponseEntity.ok(
                userService.getMyProfile()
        );
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMyProfile(
            @Valid @RequestBody ProfileUpdateDTO profileUpdateDTO) {

        return ResponseEntity.ok(
                userService.updateMyProfile(profileUpdateDTO)
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount() {

        userService.deleteMyAccount();

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO userRequestDTO) {

        UserResponseDTO response =
                userService.updateUser(id, userRequestDTO);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id) {

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}