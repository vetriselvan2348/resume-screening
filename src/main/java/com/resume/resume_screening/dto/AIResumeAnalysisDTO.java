package com.resume.resume_screening.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIResumeAnalysisDTO {

    private double overallScore;
    private double skillsScore;
    private double experienceScore;
    private double educationScore;
    private double resumeQualityScore;
    private String recommendation;
    private String summary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> missingSkills;
    private String interviewRecommendation;
}