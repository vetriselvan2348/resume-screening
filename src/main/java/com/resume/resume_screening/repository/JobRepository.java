package com.resume.resume_screening.repository;

import com.resume.resume_screening.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
    
}