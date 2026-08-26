package com.resume.resume_screening.controller;

import com.resume.resume_screening.dto.ResumeResponseDTO;
import com.resume.resume_screening.service.ResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    // Candidate: upload resume
    @PostMapping("/upload/{jobId}")
    public ResponseEntity<ResumeResponseDTO> uploadResume(
            @PathVariable Long jobId,
            @RequestParam("file") MultipartFile file) {

        try {
            ResumeResponseDTO response =
                    resumeService.uploadResume(jobId, file);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Candidate: get only their own resumes
    @GetMapping("/my")
    public ResponseEntity<List<ResumeResponseDTO>> getMyResumes() {

        return ResponseEntity.ok(
                resumeService.getMyResumes()
        );
    }

    // Recruiter: get resumes submitted for a job
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<ResumeResponseDTO>> getResumesByJob(
            @PathVariable Long jobId) {

        return ResponseEntity.ok(
                resumeService.getResumesByJobId(jobId)
        );
    }
}