package com.resume.resume_screening.controller;

import com.resume.resume_screening.dto.ScreeningResultRequestDTO;
import com.resume.resume_screening.dto.ScreeningResultResponseDTO;
import com.resume.resume_screening.service.ScreeningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/screening")
public class ScreeningController {

    private final ScreeningService screeningService;

    public ScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @PostMapping
    public ResponseEntity<ScreeningResultResponseDTO> screenResume(
            @RequestBody ScreeningResultRequestDTO request) {

        ScreeningResultResponseDTO response =
                screeningService.screenResume(request);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<ScreeningResultResponseDTO>> getResultsByJobId(
            @PathVariable Long jobId) {

        return ResponseEntity.ok(
                screeningService.getResultsByJobId(jobId)
        );
    }
}