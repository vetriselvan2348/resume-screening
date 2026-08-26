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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private ResumeService resumeService;

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
    // UPLOAD VALID PDF
    // =========================================================

    @Test
    void uploadResume_shouldUploadValidPdf()
            throws Exception {

        User candidate = new User();

        candidate.setId(1L);
        candidate.setName("Candidate");
        candidate.setEmail("candidate@test.com");

        Job job = new Job();

        job.setId(1L);
        job.setTitle("Java Developer");

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "resume.pdf",
                        "application/pdf",
                        "Java Spring Boot MySQL"
                                .getBytes()
                );

        Resume savedResume = new Resume();

        savedResume.setId(1L);
        savedResume.setFileName("resume.pdf");
        savedResume.setFileType("application/pdf");
        savedResume.setExtractedText(
                "Java Spring Boot MySQL"
        );
        savedResume.setJob(job);
        savedResume.setCandidate(candidate);

        mockLoggedInUser("candidate@test.com");

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        when(userRepository.findByEmail(
                "candidate@test.com"))
                .thenReturn(Optional.of(candidate));

        when(resumeRepository.save(any(Resume.class)))
                .thenReturn(savedResume);

        ResumeResponseDTO response =
                resumeService.uploadResume(
                        1L,
                        file
                );

        assertEquals(
                1L,
                response.getId()
        );

        assertEquals(
                "resume.pdf",
                response.getFileName()
        );

        assertEquals(
                "application/pdf",
                response.getFileType()
        );

        verify(resumeRepository)
                .save(any(Resume.class));
    }


    // =========================================================
    // EMPTY FILE
    // =========================================================

    @Test
    void uploadResume_shouldRejectEmptyFile() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "resume.pdf",
                        "application/pdf",
                        new byte[0]
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> resumeService.uploadResume(
                        1L,
                        file
                )
        );

        verify(jobRepository, never())
                .findById(anyLong());
    }


    // =========================================================
    // UNSUPPORTED FILE TYPE
    // =========================================================

    @Test
    void uploadResume_shouldRejectUnsupportedFile() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "resume.jpg",
                        "image/jpeg",
                        "image data".getBytes()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> resumeService.uploadResume(
                        1L,
                        file
                )
        );

        verify(jobRepository, never())
                .findById(anyLong());
    }


    // =========================================================
    // FILE TOO LARGE
    // =========================================================

    @Test
    void uploadResume_shouldRejectLargeFile() {

        byte[] largeFile =
                new byte[5 * 1024 * 1024 + 1];

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "large.pdf",
                        "application/pdf",
                        largeFile
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> resumeService.uploadResume(
                        1L,
                        file
                )
        );

        verify(jobRepository, never())
                .findById(anyLong());
    }


    // =========================================================
    // JOB NOT FOUND
    // =========================================================

    @Test
    void uploadResume_shouldFailWhenJobDoesNotExist() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "resume.pdf",
                        "application/pdf",
                        "Java".getBytes()
                );

        when(jobRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> resumeService.uploadResume(
                        99L,
                        file
                )
        );
    }


    // =========================================================
    // GET MY RESUMES
    // =========================================================

    @Test
    void getMyResumes_shouldReturnCandidateResumes() {

        User candidate = new User();

        candidate.setId(1L);
        candidate.setEmail(
                "candidate@test.com"
        );

        Resume resume1 = new Resume();

        resume1.setId(1L);
        resume1.setFileName("resume1.pdf");
        resume1.setFileType("application/pdf");
        resume1.setExtractedText("Java");

        Resume resume2 = new Resume();

        resume2.setId(2L);
        resume2.setFileName("resume2.pdf");
        resume2.setFileType("application/pdf");
        resume2.setExtractedText("Spring Boot");

        mockLoggedInUser(
                "candidate@test.com"
        );

        when(userRepository.findByEmail(
                "candidate@test.com"))
                .thenReturn(
                        Optional.of(candidate)
                );

        when(resumeRepository
                .findByCandidateId(1L))
                .thenReturn(
                        List.of(
                                resume1,
                                resume2
                        )
                );

        List<ResumeResponseDTO> response =
                resumeService.getMyResumes();

        assertEquals(
                2,
                response.size()
        );

        assertEquals(
                "resume1.pdf",
                response.get(0).getFileName()
        );

        assertEquals(
                "resume2.pdf",
                response.get(1).getFileName()
        );

        verify(resumeRepository)
                .findByCandidateId(1L);
    }


    // =========================================================
    // RECRUITER GETS OWN JOB RESUMES
    // =========================================================

    @Test
    void getResumesByJobId_shouldAllowJobOwner() {

        User recruiter = new User();

        recruiter.setId(1L);
        recruiter.setEmail(
                "recruiter@test.com"
        );

        Job job = new Job();

        job.setId(1L);
        job.setTitle("Java Developer");
        job.setRecruiter(recruiter);

        Resume resume = new Resume();

        resume.setId(1L);
        resume.setFileName("resume.pdf");
        resume.setFileType("application/pdf");
        resume.setExtractedText("Java Spring Boot");
        resume.setJob(job);

        when(jobRepository.findById(1L))
                .thenReturn(
                        Optional.of(job)
                );

        mockLoggedInUser(
                "recruiter@test.com"
        );

        when(userRepository.findByEmail(
                "recruiter@test.com"))
                .thenReturn(
                        Optional.of(recruiter)
                );

        when(resumeRepository.findByJobId(1L))
                .thenReturn(
                        List.of(resume)
                );

        List<ResumeResponseDTO> response =
                resumeService.getResumesByJobId(1L);

        assertEquals(
                1,
                response.size()
        );

        assertEquals(
                "resume.pdf",
                response.get(0).getFileName()
        );

        verify(resumeRepository)
                .findByJobId(1L);
    }


    // =========================================================
    // RECRUITER CANNOT ACCESS OTHER JOB
    // =========================================================

    @Test
    void getResumesByJobId_shouldRejectDifferentRecruiter() {

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
                .thenReturn(
                        Optional.of(job)
                );

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
                () -> resumeService
                        .getResumesByJobId(1L)
        );

        verify(
                resumeRepository,
                never()
        ).findByJobId(anyLong());
    }


    // =========================================================
    // SECURITY CONTEXT MOCK
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