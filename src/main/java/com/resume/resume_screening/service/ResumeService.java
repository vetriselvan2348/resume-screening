package com.resume.resume_screening.service;

import com.resume.resume_screening.dto.ResumeResponseDTO;
import com.resume.resume_screening.exception.ForbiddenException;
import com.resume.resume_screening.exception.ResourceNotFoundException;
import com.resume.resume_screening.model.Job;
import com.resume.resume_screening.model.Resume;
import com.resume.resume_screening.model.User;
import com.resume.resume_screening.repository.JobRepository;
import com.resume.resume_screening.repository.ResumeRepository;
import com.resume.resume_screening.repository.UserRepository;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final Tika tika;

    public ResumeService(
            ResumeRepository resumeRepository,
            JobRepository jobRepository,
            UserRepository userRepository) {

        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.tika = new Tika();
    }

    // =========================================================
    // CANDIDATE - UPLOAD RESUME
    // =========================================================

   public ResumeResponseDTO uploadResume(
        Long jobId,
        MultipartFile file)
        throws IOException, TikaException {

    // =========================
    // FILE VALIDATION
    // =========================

    if (file == null || file.isEmpty()) {
        throw new IllegalArgumentException(
                "Resume file is empty"
        );
    }

    // Maximum file size = 5 MB
    long maxSize = 5 * 1024 * 1024;

    if (file.getSize() > maxSize) {
        throw new IllegalArgumentException(
                "Resume file must not exceed 5 MB"
        );
    }

    String contentType = file.getContentType();

    if (contentType == null ||
            !(contentType.equals("application/pdf")
            || contentType.equals("application/msword")
            || contentType.equals(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {

        throw new IllegalArgumentException(
                "Only PDF, DOC, and DOCX files are allowed"
        );
    }

    // =========================
    // FIND JOB
    // =========================

    Job job = jobRepository.findById(jobId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Job not found"
                    ));

    // =========================
    // GET LOGGED-IN CANDIDATE
    // =========================

    User candidate = getLoggedInUser();

    // =========================
    // EXTRACT RESUME TEXT
    // =========================

    String extractedText =
            tika.parseToString(
                    file.getInputStream()
            );

    // =========================
    // CREATE RESUME
    // =========================

    Resume resume = new Resume();

    resume.setFileName(
            file.getOriginalFilename()
    );

    resume.setFileType(
            contentType
    );

    resume.setExtractedText(
            extractedText
    );

    resume.setJob(job);

    // Assign logged-in candidate
    resume.setCandidate(candidate);

    // =========================
    // SAVE
    // =========================

    Resume savedResume =
            resumeRepository.save(resume);

    return convertToDTO(savedResume);
}


    // =========================================================
    // CANDIDATE - GET MY RESUMES
    // =========================================================

    public List<ResumeResponseDTO> getMyResumes() {

        User candidate = getLoggedInUser();

        return resumeRepository
                .findByCandidateId(candidate.getId())
                .stream()
                .map(this::convertToDTO)
                .toList();
    }


    // =========================================================
    // RECRUITER - GET RESUMES FOR MY JOB
    // =========================================================

    public List<ResumeResponseDTO> getResumesByJobId(
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
                    "You are not allowed to view resumes for this job"
            );
        }

        // Return resumes
        return resumeRepository
                .findByJobId(jobId)
                .stream()
                .map(this::convertToDTO)
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


    // =========================================================
    // ENTITY -> DTO
    // =========================================================

    private ResumeResponseDTO convertToDTO(
            Resume resume) {

        return new ResumeResponseDTO(
                resume.getId(),
                resume.getFileName(),
                resume.getFileType(),
                resume.getExtractedText()
        );
    }
}