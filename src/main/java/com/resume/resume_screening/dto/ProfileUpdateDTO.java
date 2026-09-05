package com.resume.resume_screening.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateDTO {

    private String name;

    @Size(min = 6, message = "Password must contain at least 6 characters")
    private String password;
}
