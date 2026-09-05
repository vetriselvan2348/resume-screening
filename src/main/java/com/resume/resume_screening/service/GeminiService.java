package com.resume.resume_screening.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.resume.resume_screening.dto.AIResumeAnalysisDTO;
import com.resume.resume_screening.exception.ForbiddenException;
import com.resume.resume_screening.exception.ResourceNotFoundException;
import com.resume.resume_screening.model.Job;
import com.resume.resume_screening.model.Resume;
import com.resume.resume_screening.model.User;
import com.resume.resume_screening.repository.JobRepository;
import com.resume.resume_screening.repository.ResumeRepository;
import com.resume.resume_screening.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Client geminiClient;

    public GeminiService(
            JobRepository jobRepository,
            ResumeRepository resumeRepository,
            UserRepository userRepository) {

        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper();

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY environment variable is not configured"
            );
        }

        this.geminiClient = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    public AIResumeAnalysisDTO analyzeResume(
            Long jobId,
            Long resumeId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Job not found"));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Resume not found"));

        verifyAccess(job, resume);

        String prompt = buildPrompt(job, resume);

        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .responseMimeType("application/json")
                        .build();

        GenerateContentResponse response =
                geminiClient.models.generateContent(
                        "gemini-3.6-flash",
                        prompt,
                        config
                );

        String json = response.text();

        if (json == null || json.isBlank()) {
            throw new IllegalStateException(
                    "Gemini returned an empty response"
            );
        }

        json = cleanJsonResponse(json);

        try {
            return objectMapper.readValue(
                    json,
                    AIResumeAnalysisDTO.class
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse Gemini response: " + json,
                    e
            );
        }
    }

    private String buildPrompt(
            Job job,
            Resume resume) {

        return """
                You are an expert recruitment assistant.

                Analyze the candidate resume against the job requirements.

                Return ONLY valid JSON matching this exact structure:

                {
                  "overallScore": 0,
                  "skillsScore": 0,
                  "experienceScore": 0,
                  "educationScore": 0,
                  "resumeQualityScore": 0,
                  "recommendation": "",
                  "summary": "",
                  "strengths": [],
                  "weaknesses": [],
                  "missingSkills": [],
                  "interviewRecommendation": ""
                }

                Scoring rules:

                overallScore must be between 0 and 100.
                skillsScore must be between 0 and 100.
                experienceScore must be between 0 and 100.
                educationScore must be between 0 and 100.
                resumeQualityScore must be between 0 and 100.

                Evaluate only information supported by the resume.

                Do not invent candidate experience, education, skills,
                certifications, projects, achievements or technologies.

                Compare the candidate experience against the minimum
                experience required by the job.

                recommendation must be exactly one of:

                STRONG_MATCH
                GOOD_MATCH
                PARTIAL_MATCH
                WEAK_MATCH

                interviewRecommendation must be exactly one of:

                RECOMMENDED
                MAYBE
                NOT_RECOMMENDED

                Keep the summary concise.

                Strengths must contain only strengths supported by the resume.

                Weaknesses must contain only weaknesses supported by the
                comparison between the resume and the job.

                missingSkills must contain required job skills that are not
                clearly demonstrated in the resume.

                JOB TITLE:
                %s

                JOB DESCRIPTION:
                %s

                REQUIRED SKILLS:
                %s

                MINIMUM EXPERIENCE:
                %s years

                CANDIDATE RESUME:
                %s
                """.formatted(
                safe(job.getTitle()),
                safe(job.getDescription()),
                safe(job.getRequiredSkills()),
                job.getMinimumExperience(),
                safe(resume.getExtractedText())
        );
    }

    private String cleanJsonResponse(String json) {

        String cleaned = json.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(
                    0,
                    cleaned.length() - 3
            ).trim();
        }

        return cleaned;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void verifyAccess(
            Job job,
            Resume resume) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String email = authentication.getName();

        User recruiter = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (!job.getRecruiter()
                .getId()
                .equals(recruiter.getId())) {

            throw new ForbiddenException(
                    "You are not allowed to analyze this job"
            );
        }

        if (!resume.getJob()
                .getId()
                .equals(job.getId())) {

            throw new ForbiddenException(
                    "Resume does not belong to this job"
            );
        }
    }
}