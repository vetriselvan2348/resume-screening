package com.resume.resume_screening.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeRequestDTO {

    private String fileName;

    private String fileType;

    private String extractedText;
}