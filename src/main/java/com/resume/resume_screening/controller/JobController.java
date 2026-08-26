package com.resume.resume_screening.controller;

import com.resume.resume_screening.dto.JobRequestDTO;
import com.resume.resume_screening.dto.JobResponseDTO;
import com.resume.resume_screening.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponseDTO> createJob(
            @Valid @RequestBody JobRequestDTO request) {

        JobResponseDTO response = jobService.createJob(request);

        return ResponseEntity.ok(response);
    }
    @GetMapping
    public ResponseEntity<List<JobResponseDTO>> getAllJobs() {

        return ResponseEntity.ok(jobService.getAllJobs());
    }
    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDTO> getJobById(
            @PathVariable Long id) {

        return ResponseEntity.ok(jobService.getJobById(id));
    }
    @PutMapping("/{id}")
    public ResponseEntity<JobResponseDTO> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobRequestDTO request) {

        JobResponseDTO response = jobService.updateJob(id, request);

        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {

        jobService.deleteJob(id);

        return ResponseEntity.noContent().build();
    }
}