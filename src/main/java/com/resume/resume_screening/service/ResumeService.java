package com.resume.resume_screening.service;

import com.resume.resume_screening.dto.ApplicantResponseDTO;
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

    public ResumeResponseDTO uploadResume(
            Long jobId,
            MultipartFile file)
            throws IOException, TikaException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Resume file is empty"
            );
        }

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

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job not found"
                        ));

        User candidate = getLoggedInUser();

        String extractedText =
                tika.parseToString(
                        file.getInputStream()
                );

        Resume resume = new Resume();

        resume.setFileName(
                file.getOriginalFilename()
        );

        resume.setFileType(
                contentType
        );

        resume.setFileData(
                file.getBytes()
        );

        resume.setExtractedText(
                extractedText
        );

        resume.setJob(job);

        resume.setCandidate(candidate);

        Resume savedResume =
                resumeRepository.save(resume);

        return convertToDTO(savedResume);
    }

    public List<ResumeResponseDTO> getMyResumes() {

        User candidate = getLoggedInUser();

        return resumeRepository
                .findByCandidateId(candidate.getId())
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<ResumeResponseDTO> getResumesByJobId(
            Long jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job not found"
                        ));

        User recruiter = getLoggedInUser();

        if (!job.getRecruiter()
                .getId()
                .equals(recruiter.getId())) {

            throw new ForbiddenException(
                    "You are not allowed to view resumes for this job"
            );
        }

        return resumeRepository
                .findByJobId(jobId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<ApplicantResponseDTO> getApplicantsByJobId(
            Long jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job not found"
                        ));

        User recruiter = getLoggedInUser();

        if (!job.getRecruiter()
                .getId()
                .equals(recruiter.getId())) {

            throw new ForbiddenException(
                    "You are not allowed to view applicants for this job"
            );
        }

        return resumeRepository
                .findByJobId(jobId)
                .stream()
                .map(this::convertToApplicantDTO)
                .toList();
    }

    public void deleteResume(Long resumeId) {

        User candidate = getLoggedInUser();

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resume not found"
                        ));

        if (!resume.getCandidate()
                .getId()
                .equals(candidate.getId())) {

            throw new ForbiddenException(
                    "You are not allowed to delete this resume"
            );
        }

        resumeRepository.delete(resume);
    }

    public byte[] getResumeFile(Long resumeId) {

        User currentUser = getLoggedInUser();

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resume not found"
                        ));

        if (resume.getCandidate()
                .getId()
                .equals(currentUser.getId())) {

            return resume.getFileData();
        }

        if (resume.getJob()
                .getRecruiter()
                .getId()
                .equals(currentUser.getId())) {

            return resume.getFileData();
        }

        throw new ForbiddenException(
                "You are not allowed to access this resume"
        );
    }

    public String getResumeFileType(Long resumeId) {

        User currentUser = getLoggedInUser();

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resume not found"
                        ));

        if (resume.getCandidate()
                .getId()
                .equals(currentUser.getId()) ||
                resume.getJob()
                        .getRecruiter()
                        .getId()
                        .equals(currentUser.getId())) {

            return resume.getFileType();
        }

        throw new ForbiddenException(
                "You are not allowed to access this resume"
        );
    }

    private ApplicantResponseDTO convertToApplicantDTO(
            Resume resume) {

        User candidate = resume.getCandidate();
        Job job = resume.getJob();

        return new ApplicantResponseDTO(
                resume.getId(),
                candidate.getId(),
                candidate.getName(),
                candidate.getEmail(),
                resume.getFileName(),
                resume.getFileType(),
                resume.getExtractedText(),
                job.getId(),
                job.getTitle()
        );
    }

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