package com.farmrakshak.crop.repository;

import com.farmrakshak.crop.entity.DiseaseAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface DiseaseAlertRepository extends JpaRepository<DiseaseAlert, UUID> {

    /**
     * Check if a similar disease alert was already raised recently (deduplication).
     * Prevents spam if multiple farmers in the same district report the same disease
     * within a short time window.
     */
    boolean existsByDiseaseNameAndDistrictAndCreatedAtAfter(
            String diseaseName, String district, Instant after);
}
