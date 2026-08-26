package com.resume.resume_screening.service;

import com.resume.resume_screening.dto.JobRequestDTO;
import com.resume.resume_screening.dto.JobResponseDTO;
import com.resume.resume_screening.exception.ForbiddenException;
import com.resume.resume_screening.exception.ResourceNotFoundException;
import com.resume.resume_screening.model.Job;
import com.resume.resume_screening.model.User;
import com.resume.resume_screening.repository.JobRepository;
import com.resume.resume_screening.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private JobService jobService;

    private MockedStatic<SecurityContextHolder> securityContextMock;


    @AfterEach
    void clearSecurityContext() {

        if (securityContextMock != null) {
            securityContextMock.close();
        }

        SecurityContextHolder.clearContext();
    }


    // =========================================================
    // CREATE JOB
    // =========================================================

    @Test
    void createJob_shouldCreateJobForLoggedInRecruiter() {

        User recruiter = new User();

        recruiter.setId(1L);
        recruiter.setName("Recruiter");
        recruiter.setEmail("recruiter@test.com");

        JobRequestDTO request = new JobRequestDTO(
                "Java Developer",
                "Backend developer",
                "Java, Spring Boot, MySQL",
                1
        );

        Job savedJob = new Job();

        savedJob.setId(1L);
        savedJob.setTitle("Java Developer");
        savedJob.setDescription("Backend developer");
        savedJob.setRequiredSkills(
                "Java, Spring Boot, MySQL"
        );
        savedJob.setMinimumExperience(1);
        savedJob.setRecruiter(recruiter);

        mockLoggedInUser("recruiter@test.com");

        when(userRepository.findByEmail(
                "recruiter@test.com"))
                .thenReturn(Optional.of(recruiter));

        when(jobRepository.save(any(Job.class)))
                .thenReturn(savedJob);

        JobResponseDTO response =
                jobService.createJob(request);

        assertEquals(1L, response.getId());

        assertEquals(
                "Java Developer",
                response.getTitle()
        );

        assertEquals(
                "Backend developer",
                response.getDescription()
        );

        assertEquals(
                "Java, Spring Boot, MySQL",
                response.getRequiredSkills()
        );

        assertEquals(
                1,
                response.getMinimumExperience()
        );

        verify(jobRepository)
                .save(any(Job.class));
    }


    // =========================================================
    // GET JOB BY ID
    // =========================================================

    @Test
    void getJobById_shouldReturnJob() {

        Job job = new Job();

        job.setId(1L);
        job.setTitle("Java Developer");
        job.setDescription("Backend developer");
        job.setRequiredSkills(
                "Java, Spring Boot"
        );
        job.setMinimumExperience(1);

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        JobResponseDTO response =
                jobService.getJobById(1L);

        assertEquals(
                1L,
                response.getId()
        );

        assertEquals(
                "Java Developer",
                response.getTitle()
        );

        verify(jobRepository)
                .findById(1L);
    }


    // =========================================================
    // GET JOB - NOT FOUND
    // =========================================================

    @Test
    void getJobById_shouldThrowWhenJobDoesNotExist() {

        when(jobRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> jobService.getJobById(99L)
        );

        verify(jobRepository)
                .findById(99L);
    }


    // =========================================================
    // GET ALL JOBS
    // =========================================================

    @Test
    void getAllJobs_shouldReturnAllJobs() {

        Job job1 = new Job();

        job1.setId(1L);
        job1.setTitle("Java Developer");

        Job job2 = new Job();

        job2.setId(2L);
        job2.setTitle("Spring Boot Developer");

        when(jobRepository.findAll())
                .thenReturn(
                        List.of(job1, job2)
                );

        List<JobResponseDTO> response =
                jobService.getAllJobs();

        assertEquals(
                2,
                response.size()
        );

        assertEquals(
                "Java Developer",
                response.get(0).getTitle()
        );

        assertEquals(
                "Spring Boot Developer",
                response.get(1).getTitle()
        );

        verify(jobRepository)
                .findAll();
    }


    // =========================================================
    // UPDATE JOB - OWNER
    // =========================================================

    @Test
    void updateJob_shouldAllowJobOwner() {

        User recruiter = new User();

        recruiter.setId(1L);
        recruiter.setEmail("recruiter@test.com");

        Job job = new Job();

        job.setId(1L);
        job.setTitle("Old Title");
        job.setRecruiter(recruiter);

        JobRequestDTO request = new JobRequestDTO(
                "Updated Java Developer",
                "Updated description",
                "Java, Spring Boot, MySQL",
                2
        );

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        mockLoggedInUser("recruiter@test.com");

        when(userRepository.findByEmail(
                "recruiter@test.com"))
                .thenReturn(Optional.of(recruiter));

        when(jobRepository.save(any(Job.class)))
                .thenReturn(job);

        JobResponseDTO response =
                jobService.updateJob(
                        1L,
                        request
                );

        assertEquals(
                "Updated Java Developer",
                response.getTitle()
        );

        assertEquals(
                "Updated description",
                response.getDescription()
        );

        verify(jobRepository)
                .save(job);
    }


    // =========================================================
    // UPDATE JOB - DIFFERENT RECRUITER
    // =========================================================

    @Test
    void updateJob_shouldRejectDifferentRecruiter() {

        User owner = new User();

        owner.setId(1L);

        User otherRecruiter = new User();

        otherRecruiter.setId(2L);
        otherRecruiter.setEmail(
                "other@test.com"
        );

        Job job = new Job();

        job.setId(1L);
        job.setRecruiter(owner);

        JobRequestDTO request = new JobRequestDTO(
                "Updated Job",
                "Updated description",
                "Java",
                1
        );

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        mockLoggedInUser("other@test.com");

        when(userRepository.findByEmail(
                "other@test.com"))
                .thenReturn(
                        Optional.of(otherRecruiter)
                );

        assertThrows(
                ForbiddenException.class,
                () -> jobService.updateJob(
                        1L,
                        request
                )
        );

        verify(
                jobRepository,
                never()
        ).save(any(Job.class));
    }


    // =========================================================
    // DELETE JOB - OWNER
    // =========================================================

    @Test
    void deleteJob_shouldAllowJobOwner() {

        User recruiter = new User();

        recruiter.setId(1L);
        recruiter.setEmail(
                "recruiter@test.com"
        );

        Job job = new Job();

        job.setId(1L);
        job.setRecruiter(recruiter);

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        mockLoggedInUser(
                "recruiter@test.com"
        );

        when(userRepository.findByEmail(
                "recruiter@test.com"))
                .thenReturn(
                        Optional.of(recruiter)
                );

        jobService.deleteJob(1L);

        verify(jobRepository)
                .delete(job);
    }


    // =========================================================
    // DELETE JOB - DIFFERENT RECRUITER
    // =========================================================

    @Test
    void deleteJob_shouldRejectDifferentRecruiter() {

        User owner = new User();

        owner.setId(1L);

        User otherRecruiter = new User();

        otherRecruiter.setId(2L);
        otherRecruiter.setEmail(
                "other@test.com"
        );

        Job job = new Job();

        job.setId(1L);
        job.setRecruiter(owner);

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        mockLoggedInUser(
                "other@test.com"
        );

        when(userRepository.findByEmail(
                "other@test.com"))
                .thenReturn(
                        Optional.of(otherRecruiter)
                );

        assertThrows(
                ForbiddenException.class,
                () -> jobService.deleteJob(1L)
        );

        verify(
                jobRepository,
                never()
        ).delete(any(Job.class));
    }


    // =========================================================
    // MOCK LOGGED-IN USER
    // =========================================================

    private void mockLoggedInUser(
            String email) {

        when(authentication.getName())
                .thenReturn(email);

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        securityContextMock =
                mockStatic(
                        SecurityContextHolder.class
                );

        securityContextMock
                .when(
                        SecurityContextHolder::getContext
                )
                .thenReturn(securityContext);
    }
}