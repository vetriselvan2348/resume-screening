package com.resume.resume_screening.service;

import com.resume.resume_screening.dto.JobRequestDTO;
import com.resume.resume_screening.dto.JobResponseDTO;
import com.resume.resume_screening.exception.ForbiddenException;
import com.resume.resume_screening.exception.ResourceNotFoundException;
import com.resume.resume_screening.model.Job;
import com.resume.resume_screening.model.User;
import com.resume.resume_screening.repository.JobRepository;
import com.resume.resume_screening.repository.UserRepository;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public JobService(
            JobRepository jobRepository,
            UserRepository userRepository) {

        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    public JobResponseDTO createJob(JobRequestDTO request) {

        // Get logged-in user's email from JWT
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String email = authentication.getName();

        // Find recruiter
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Recruiter not found"
                        ));

        Job job = new Job();

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setMinimumExperience(request.getMinimumExperience());

        // Set logged-in recruiter as owner
        job.setRecruiter(recruiter);

        Job savedJob = jobRepository.save(job);

        return new JobResponseDTO(
                savedJob.getId(),
                savedJob.getTitle(),
                savedJob.getDescription(),
                savedJob.getRequiredSkills(),
                savedJob.getMinimumExperience()
        );
    }

    public List<JobResponseDTO> getAllJobs() {

        return jobRepository.findAll()
                .stream()
                .map(job -> new JobResponseDTO(
                        job.getId(),
                        job.getTitle(),
                        job.getDescription(),
                        job.getRequiredSkills(),
                        job.getMinimumExperience()
                ))
                .toList();
    }

    public JobResponseDTO getJobById(Long id) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job not found"
                        ));

        return new JobResponseDTO(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getRequiredSkills(),
                job.getMinimumExperience()
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
                updatedJob.getMinimumExperience()
        );
        }
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

        jobRepository.delete(job);
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