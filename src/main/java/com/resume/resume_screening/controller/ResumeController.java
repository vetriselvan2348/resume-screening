package com.resume.resume_screening.controller;

import com.resume.resume_screening.dto.ApplicantResponseDTO;
import com.resume.resume_screening.dto.ResumeResponseDTO;
import com.resume.resume_screening.service.ResumeService;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @GetMapping("/my")
    public ResponseEntity<List<ResumeResponseDTO>> getMyResumes() {

        return ResponseEntity.ok(
                resumeService.getMyResumes()
        );
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<ResumeResponseDTO>> getResumesByJob(
            @PathVariable Long jobId) {

        return ResponseEntity.ok(
                resumeService.getResumesByJobId(jobId)
        );
    }

    @GetMapping("/job/{jobId}/applicants")
    public ResponseEntity<List<ApplicantResponseDTO>> getApplicantsByJob(
            @PathVariable Long jobId) {

        return ResponseEntity.ok(
                resumeService.getApplicantsByJobId(jobId)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable Long id) {

        resumeService.deleteResume(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadResume(
            @PathVariable Long id) {

        byte[] fileData =
                resumeService.getResumeFile(id);

        String fileType =
                resumeService.getResumeFileType(id);

        MediaType mediaType;

        try {
            mediaType =
                    MediaType.parseMediaType(fileType);
        } catch (Exception e) {
            mediaType =
                    MediaType.APPLICATION_OCTET_STREAM;
        }

        ByteArrayResource resource =
                new ByteArrayResource(fileData);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(fileData.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline"
                )
                .body(resource);
    }
}