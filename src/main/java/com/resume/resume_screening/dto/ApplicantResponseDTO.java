package com.resume.resume_screening.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantResponseDTO {

    private Long resumeId;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private String fileName;
    private String fileType;
    private String extractedText;
    private Long jobId;
    private String jobTitle;
}