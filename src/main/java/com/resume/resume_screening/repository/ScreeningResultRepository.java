package com.resume.resume_screening.repository;

import com.resume.resume_screening.model.ScreeningResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScreeningResultRepository
        extends JpaRepository<ScreeningResult, Long> {

    List<ScreeningResult> findByJobIdOrderByScoreDesc(Long jobId);
    Optional<ScreeningResult> findByJobIdAndResumeId(Long jobId, Long resumeId);
    void deleteByJobId(Long jobId);
}