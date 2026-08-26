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
class ScreeningServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ScreeningResultRepository screeningResultRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private ScreeningService screeningService;

    private MockedStatic<SecurityContextHolder>
            securityContextMock;


    @AfterEach
    void cleanup() {

        if (securityContextMock != null) {
            securityContextMock.close();
        }

        SecurityContextHolder.clearContext();
    }


    // =========================================================
    // SCREEN RESUME - SUCCESS
    // =========================================================

    @Test
    void screenResume_shouldCalculateCorrectScore() {

        User recruiter = new User();
        recruiter.setId(1L);
        recruiter.setEmail("recruiter@test.com");

        Job job = new Job();
        job.setId(1L);
        job.setTitle("Java Developer");
        job.setRecruiter(recruiter);
        job.setRequiredSkills(
                "Java, Spring Boot, MySQL, Docker"
        );

        Resume resume = new Resume();
        resume.setId(1L);
        resume.setJob(job);
        resume.setExtractedText(
                "I have experience with Java, Spring Boot and MySQL."
        );

        ScreeningResult savedResult =
                new ScreeningResult();

        savedResult.setId(1L);
        savedResult.setJob(job);
        savedResult.setResume(resume);
        savedResult.setScore(75.0);
        savedResult.setMatchedSkills(
                "java, spring boot, mysql"
        );
        savedResult.setMissingSkills(
                "docker"
        );

        ScreeningResultRequestDTO request =
                new ScreeningResultRequestDTO(
                        1L,
                        1L
                );

        mockLoggedInUser(
                "recruiter@test.com"
        );

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        when(userRepository.findByEmail(
                "recruiter@test.com"))
                .thenReturn(Optional.of(recruiter));

        when(resumeRepository.findById(1L))
                .thenReturn(Optional.of(resume));

        when(screeningResultRepository
                .findByJobIdAndResumeId(1L, 1L))
                .thenReturn(Optional.empty());

        when(screeningResultRepository.save(
                any(ScreeningResult.class)))
                .thenReturn(savedResult);

        ScreeningResultResponseDTO response =
                screeningService.screenResume(request);

        assertEquals(
                1L,
                response.getId()
        );

        assertEquals(
                1L,
                response.getJobId()
        );

        assertEquals(
                1L,
                response.getResumeId()
        );

        assertEquals(
                75.0,
                response.getScore()
        );

        assertEquals(
                "java, spring boot, mysql",
                response.getMatchedSkills()
        );

        assertEquals(
                "docker",
                response.getMissingSkills()
        );

        verify(screeningResultRepository)
                .save(any(ScreeningResult.class));
    }


    // =========================================================
    // SCREEN RESUME - JOB NOT FOUND
    // =========================================================

    @Test
    void screenResume_shouldFailWhenJobNotFound() {

        ScreeningResultRequestDTO request =
                new ScreeningResultRequestDTO(
                        99L,
                        1L
                );

        when(jobRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> screeningService
                        .screenResume(request)
        );

        verify(
                resumeRepository,
                never()
        ).findById(anyLong());
    }


    // =========================================================
    // SCREEN RESUME - RESUME NOT FOUND
    // =========================================================

    @Test
    void screenResume_shouldFailWhenResumeNotFound() {

        User recruiter = new User();

        recruiter.setId(1L);
        recruiter.setEmail(
                "recruiter@test.com"
        );

        Job job = new Job();

        job.setId(1L);
        job.setRecruiter(recruiter);
        job.setRequiredSkills(
                "Java, Spring Boot"
        );

        ScreeningResultRequestDTO request =
                new ScreeningResultRequestDTO(
                        1L,
                        99L
                );

        mockLoggedInUser(
                "recruiter@test.com"
        );

        when(jobRepository.findById(1L))
                .thenReturn(
                        Optional.of(job)
                );

        when(userRepository.findByEmail(
                "recruiter@test.com"))
                .thenReturn(
                        Optional.of(recruiter)
                );

        when(resumeRepository.findById(99L))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> screeningService
                        .screenResume(request)
        );
    }


    // =========================================================
    // SCREEN RESUME - DIFFERENT RECRUITER
    // =========================================================

    @Test
    void screenResume_shouldRejectDifferentRecruiter() {

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

        ScreeningResultRequestDTO request =
                new ScreeningResultRequestDTO(
                        1L,
                        1L
                );

        mockLoggedInUser(
                "other@test.com"
        );

        when(jobRepository.findById(1L))
                .thenReturn(
                        Optional.of(job)
                );

        when(userRepository.findByEmail(
                "other@test.com"))
                .thenReturn(
                        Optional.of(otherRecruiter)
                );

        assertThrows(
                ForbiddenException.class,
                () -> screeningService
                        .screenResume(request)
        );

        verify(
                resumeRepository,
                never()
        ).findById(anyLong());
    }


    // =========================================================
    // SCREEN RESUME - RESUME BELONGS TO DIFFERENT JOB
    // =========================================================

    @Test
    void screenResume_shouldRejectResumeFromDifferentJob() {

        User recruiter = new User();

        recruiter.setId(1L);
        recruiter.setEmail(
                "recruiter@test.com"
        );

        Job job1 = new Job();
        job1.setId(1L);
        job1.setRecruiter(recruiter);
        job1.setRequiredSkills("Java");

        Job job2 = new Job();
        job2.setId(2L);
        job2.setRecruiter(recruiter);

        Resume resume = new Resume();

        resume.setId(1L);
        resume.setJob(job2);
        resume.setExtractedText("Java");

        ScreeningResultRequestDTO request =
                new ScreeningResultRequestDTO(
                        1L,
                        1L
                );

        mockLoggedInUser(
                "recruiter@test.com"
        );

        when(jobRepository.findById(1L))
                .thenReturn(
                        Optional.of(job1)
                );

        when(userRepository.findByEmail(
                "recruiter@test.com"))
                .thenReturn(
                        Optional.of(recruiter)
                );

        when(resumeRepository.findById(1L))
                .thenReturn(
                        Optional.of(resume)
                );

        assertThrows(
                ForbiddenException.class,
                () -> screeningService
                        .screenResume(request)
        );

        verify(
                screeningResultRepository,
                never()
        ).save(any(ScreeningResult.class));
    }


    // =========================================================
    // EXISTING SCREENING RESULT
    // =========================================================

    @Test
    void screenResume_shouldUpdateExistingResult() {

        User recruiter = new User();

        recruiter.setId(1L);
        recruiter.setEmail(
                "recruiter@test.com"
        );

        Job job = new Job();

        job.setId(1L);
        job.setRecruiter(recruiter);
        job.setRequiredSkills(
                "Java, Spring Boot"
        );

        Resume resume = new Resume();

        resume.setId(1L);
        resume.setJob(job);
        resume.setExtractedText(
                "Java Spring Boot"
        );

        ScreeningResult existingResult =
                new ScreeningResult();

        existingResult.setId(10L);
        existingResult.setJob(job);
        existingResult.setResume(resume);

        ScreeningResult savedResult =
                new ScreeningResult();

        savedResult.setId(10L);
        savedResult.setJob(job);
        savedResult.setResume(resume);
        savedResult.setScore(100.0);
        savedResult.setMatchedSkills(
                "java, spring boot"
        );
        savedResult.setMissingSkills("");

        ScreeningResultRequestDTO request =
                new ScreeningResultRequestDTO(
                        1L,
                        1L
                );

        mockLoggedInUser(
                "recruiter@test.com"
        );

        when(jobRepository.findById(1L))
                .thenReturn(
                        Optional.of(job)
                );

        when(userRepository.findByEmail(
                "recruiter@test.com"))
                .thenReturn(
                        Optional.of(recruiter)
                );

        when(resumeRepository.findById(1L))
                .thenReturn(
                        Optional.of(resume)
                );

        when(screeningResultRepository
                .findByJobIdAndResumeId(1L, 1L))
                .thenReturn(
                        Optional.of(existingResult)
                );

        when(screeningResultRepository.save(
                existingResult))
                .thenReturn(savedResult);

        ScreeningResultResponseDTO response =
                screeningService.screenResume(
                        request
                );

        assertEquals(
                100.0,
                response.getScore()
        );

        verify(
                screeningResultRepository
        ).save(existingResult);
    }


    // =========================================================
    // GET RESULTS - SUCCESS
    // =========================================================

    @Test
    void getResultsByJobId_shouldReturnResults() {

        User recruiter = new User();

        recruiter.setId(1L);
        recruiter.setEmail(
                "recruiter@test.com"
        );

        Job job = new Job();

        job.setId(1L);
        job.setRecruiter(recruiter);

        Resume resume = new Resume();

        resume.setId(1L);
        resume.setJob(job);

        ScreeningResult result =
                new ScreeningResult();

        result.setId(1L);
        result.setJob(job);
        result.setResume(resume);
        result.setScore(80.0);
        result.setMatchedSkills(
                "java, spring boot"
        );
        result.setMissingSkills(
                "docker"
        );

        mockLoggedInUser(
                "recruiter@test.com"
        );

        when(jobRepository.findById(1L))
                .thenReturn(
                        Optional.of(job)
                );

        when(userRepository.findByEmail(
                "recruiter@test.com"))
                .thenReturn(
                        Optional.of(recruiter)
                );

        when(screeningResultRepository
                .findByJobIdOrderByScoreDesc(1L))
                .thenReturn(
                        List.of(result)
                );

        List<ScreeningResultResponseDTO> response =
                screeningService
                        .getResultsByJobId(1L);

        assertEquals(
                1,
                response.size()
        );

        assertEquals(
                80.0,
                response.get(0).getScore()
        );

        verify(
                screeningResultRepository
        ).findByJobIdOrderByScoreDesc(1L);
    }


    // =========================================================
    // GET RESULTS - DIFFERENT RECRUITER
    // =========================================================

    @Test
    void getResultsByJobId_shouldRejectDifferentRecruiter() {

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

        mockLoggedInUser(
                "other@test.com"
        );

        when(jobRepository.findById(1L))
                .thenReturn(
                        Optional.of(job)
                );

        when(userRepository.findByEmail(
                "other@test.com"))
                .thenReturn(
                        Optional.of(otherRecruiter)
                );

        assertThrows(
                ForbiddenException.class,
                () -> screeningService
                        .getResultsByJobId(1L)
        );

        verify(
                screeningResultRepository,
                never()
        ).findByJobIdOrderByScoreDesc(anyLong());
    }


    // =========================================================
    // GET RESULTS - JOB NOT FOUND
    // =========================================================

    @Test
    void getResultsByJobId_shouldFailWhenJobNotFound() {

        when(jobRepository.findById(99L))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> screeningService
                        .getResultsByJobId(99L)
        );
    }


    // =========================================================
    // SECURITY CONTEXT
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
                .thenReturn(
                        securityContext
                );
    }
}