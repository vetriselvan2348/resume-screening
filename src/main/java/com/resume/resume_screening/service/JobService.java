package com.resume.resume_screening.service;

import com.resume.resume_screening.dto.JobRequestDTO;
import com.resume.resume_screening.dto.JobResponseDTO;
import com.resume.resume_screening.exception.ForbiddenException;
import com.resume.resume_screening.exception.ResourceNotFoundException;
import com.resume.resume_screening.model.Job;
import com.resume.resume_screening.model.User;
import com.resume.resume_screening.repository.JobRepository;
import com.resume.resume_screening.repository.ResumeRepository;
import com.resume.resume_screening.repository.ScreeningResultRepository;
import com.resume.resume_screening.repository.UserRepository;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ScreeningResultRepository screeningResultRepository;

    public JobService(
            JobRepository jobRepository,
            UserRepository userRepository,
            ResumeRepository resumeRepository,
            ScreeningResultRepository screeningResultRepository) {

        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.screeningResultRepository = screeningResultRepository;
    }

    public JobResponseDTO createJob(JobRequestDTO request) {

        User recruiter = getLoggedInUser();

        Job job = new Job();

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setMinimumExperience(request.getMinimumExperience());
        job.setRecruiter(recruiter);

        Job savedJob = jobRepository.save(job);

        return new JobResponseDTO(
                savedJob.getId(),
                savedJob.getTitle(),
                savedJob.getDescription(),
                savedJob.getRequiredSkills(),
                savedJob.getMinimumExperience(),
                false
        );
    }

    public List<JobResponseDTO> getAllJobs() {

        User currentUser = getLoggedInUser();

        return jobRepository.findAll()
                .stream()
                .map(job -> {

                    boolean alreadyApplied = false;

                    if (currentUser.getRole().name().equals("CANDIDATE")) {
                        alreadyApplied =
                                resumeRepository.existsByCandidateIdAndJobId(
                                        currentUser.getId(),
                                        job.getId()
                                );
                    }

                    return new JobResponseDTO(
                            job.getId(),
                            job.getTitle(),
                            job.getDescription(),
                            job.getRequiredSkills(),
                            job.getMinimumExperience(),
                            alreadyApplied
                    );
                })
                .toList();
    }

    public JobResponseDTO getJobById(Long id) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job not found"
                        ));

        User currentUser = getLoggedInUser();

        boolean alreadyApplied = false;

        if (currentUser.getRole().name().equals("CANDIDATE")) {
            alreadyApplied =
                    resumeRepository.existsByCandidateIdAndJobId(
                            currentUser.getId(),
                            job.getId()
                    );
        }

        return new JobResponseDTO(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getRequiredSkills(),
                job.getMinimumExperience(),
                alreadyApplied
        );
    }

    public JobResponseDTO updateJob(
            Long id,
            JobRequestDTO request) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job not found"
                        ));

        User currentUser = getLoggedInUser();

        if (!job.getRecruiter().getId()
                .equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You are not allowed to modify this job"
            );
        }

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setMinimumExperience(request.getMinimumExperience());

        Job updatedJob = jobRepository.save(job);

        return new JobResponseDTO(
                updatedJob.getId(),
                updatedJob.getTitle(),
                updatedJob.getDescription(),
                updatedJob.getRequiredSkills(),
                updatedJob.getMinimumExperience(),
                false
        );
    }

    @Transactional
    public void deleteJob(Long id) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job not found"
                        ));

        User currentUser = getLoggedInUser();

        if (!job.getRecruiter().getId()
                .equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You are not allowed to delete this job"
            );
        }

        screeningResultRepository.deleteByJobId(id);
        screeningResultRepository.flush();

        resumeRepository.deleteByJobId(id);
        resumeRepository.flush();

        jobRepository.delete(job);
        jobRepository.flush();
    }

    private User getLoggedInUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));
    }
}