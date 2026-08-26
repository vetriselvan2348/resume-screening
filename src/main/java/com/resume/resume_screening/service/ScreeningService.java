package com.resume.resume_screening.service;

import com.resume.resume_screening.dto.ScreeningResultRequestDTO;
import com.resume.resume_screening.dto.ScreeningResultResponseDTO;
import com.resume.resume_screening.exception.ForbiddenException;
import com.resume.resume_screening.exception.ResourceNotFoundException;
import com.resume.resume_screening.model.Job;
import com.resume.resume_screening.model.Resume;
import com.resume.resume_screening.model.ScreeningResult;
import com.resume.resume_screening.model.User;
import com.resume.resume_screening.repository.JobRepository;
import com.resume.resume_screening.repository.ResumeRepository;
import com.resume.resume_screening.repository.ScreeningResultRepository;
import com.resume.resume_screening.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScreeningService {

    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final ScreeningResultRepository screeningResultRepository;
    private final UserRepository userRepository;

    public ScreeningService(
            JobRepository jobRepository,
            ResumeRepository resumeRepository,
            ScreeningResultRepository screeningResultRepository,
            UserRepository userRepository) {

        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.screeningResultRepository = screeningResultRepository;
        this.userRepository = userRepository;
    }

    // =========================================================
    // SCREEN RESUME
    // =========================================================

    public ScreeningResultResponseDTO screenResume(
            ScreeningResultRequestDTO request) {

        // Find job
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job not found"
                        ));

        // Get logged-in recruiter
        User recruiter = getLoggedInUser();

        // Check job ownership
        if (!job.getRecruiter()
                .getId()
                .equals(recruiter.getId())) {

            throw new ForbiddenException(
                    "You are not allowed to screen resumes for this job"
            );
        }

        // Find resume
        Resume resume = resumeRepository
                .findById(request.getResumeId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resume not found"
                        ));

        // Make sure resume belongs to this job
        if (!resume.getJob()
                .getId()
                .equals(job.getId())) {

            throw new ForbiddenException(
                    "Resume does not belong to this job"
            );
        }

        // Get resume text
        String resumeText =
                resume.getExtractedText()
                        .toLowerCase();

        // Get required skills
        String[] requiredSkills =
                job.getRequiredSkills()
                        .toLowerCase()
                        .split(",");

        List<String> matchedSkills =
                new ArrayList<>();

        List<String> missingSkills =
                new ArrayList<>();

        // Match skills
        for (String skill : requiredSkills) {

            skill = skill.trim();

            if (resumeText.contains(skill)) {

                matchedSkills.add(skill);

            } else {

                missingSkills.add(skill);
            }
        }

        // Calculate score
        double score = 0;

        if (requiredSkills.length > 0) {

            score =
                    ((double) matchedSkills.size()
                            / requiredSkills.length)
                            * 100;
        }

        // Find existing result or create new one
        ScreeningResult result =
                screeningResultRepository
                        .findByJobIdAndResumeId(
                                job.getId(),
                                resume.getId()
                        )
                        .orElse(new ScreeningResult());

        result.setJob(job);
        result.setResume(resume);
        result.setScore(score);
        result.setMatchedSkills(
                String.join(", ", matchedSkills)
        );
        result.setMissingSkills(
                String.join(", ", missingSkills)
        );

        // Save result
        ScreeningResult savedResult =
                screeningResultRepository.save(result);

        return new ScreeningResultResponseDTO(
                savedResult.getId(),
                job.getId(),
                resume.getId(),
                savedResult.getScore(),
                savedResult.getMatchedSkills(),
                savedResult.getMissingSkills()
        );
    }


    // =========================================================
    // GET SCREENING RESULTS FOR MY JOB
    // =========================================================

    public List<ScreeningResultResponseDTO> getResultsByJobId(
            Long jobId) {

        // Find job
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job not found"
                        ));

        // Get logged-in recruiter
        User recruiter = getLoggedInUser();

        // Check ownership
        if (!job.getRecruiter()
                .getId()
                .equals(recruiter.getId())) {

            throw new ForbiddenException(
                    "You are not allowed to view screening results for this job"
            );
        }

        // Get results
        List<ScreeningResult> results =
                screeningResultRepository
                        .findByJobIdOrderByScoreDesc(jobId);

        return results.stream()
                .map(result ->
                        new ScreeningResultResponseDTO(
                                result.getId(),
                                result.getJob().getId(),
                                result.getResume().getId(),
                                result.getScore(),
                                result.getMatchedSkills(),
                                result.getMissingSkills()
                        )
                )
                .toList();
    }


    // =========================================================
    // GET LOGGED-IN USER
    // =========================================================

    private User getLoggedInUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String email = authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));
    }
}