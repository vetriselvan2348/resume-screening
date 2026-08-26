package com.resume.resume_screening.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningResultResponseDTO {

    private Long id;

    private Long jobId;

    private Long resumeId;

    private Double score;

    private String matchedSkills;

    private String missingSkills;
}