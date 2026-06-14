package com.farmrakshak.crop.service;

import com.farmrakshak.crop.entity.CropUpload;
import com.farmrakshak.crop.entity.DiseaseAlert;
import com.farmrakshak.crop.entity.Farm;
import com.farmrakshak.crop.entity.FarmCrop;
import com.farmrakshak.crop.repository.CropUploadRepository;
import com.farmrakshak.crop.repository.DiseaseAlertRepository;
import com.farmrakshak.crop.repository.FarmCropRepository;
import com.farmrakshak.crop.repository.FarmRepository;
import com.farmrakshak.shared.constants.KafkaTopics;
import com.farmrakshak.shared.event.AnalysisResultEvent;
import com.farmrakshak.shared.event.DiseaseAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Broadcasts disease alerts to all farmers within 100km radius
 * who are growing the same crop.
 *
 * Flow:
 * 1. Disease detected via AI analysis → processAnalysisResult() triggers this
 * 2. Resolve the reporter's farm location from CropUpload → FarmCrop → Farm
 * 3. Query all farms growing the same crop (excluding reporter)
 * 4. Filter by Haversine distance ≤ 100km
 * 5. Deduplicate: skip if same disease+district was alerted in last 24h
 * 6. Publish DiseaseAlertEvent per nearby farmer via Kafka
 * 7. Record the outbreak in disease_alerts table
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiseaseAlertService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double ALERT_RADIUS_KM = 100.0;
    private static final int DEDUP_HOURS = 24;

    private final FarmRepository farmRepository;
    private final FarmCropRepository farmCropRepository;
    private final CropUploadRepository uploadRepository;
    private final DiseaseAlertRepository diseaseAlertRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Called after a disease is detected. Runs async to avoid blocking
     * the analysis result processing.
     */
    @Async
    public void broadcastDiseaseAlert(AnalysisResultEvent event) {
        try {
            String diseaseName = event.getDiseaseName();
            UUID userId = UUID.fromString(event.getUserId());
            UUID uploadId = UUID.fromString(event.getAnalysisId());

            // 1. Resolve reporter's farm location
            CropUpload upload = uploadRepository.findById(uploadId).orElse(null);
            if (upload == null) {
                log.warn("Cannot broadcast disease alert: upload not found for {}", uploadId);
                return;
            }

            Farm reporterFarm = null;
            String cropName = null;

            if (upload.getUserCropId() != null) {
                // Path A: userCropId is set → resolve via FarmCrop → Farm
                FarmCrop farmCrop = farmCropRepository.findByIdAndDeletedFalse(upload.getUserCropId()).orElse(null);
                if (farmCrop != null) {
                    reporterFarm = farmRepository.findByIdAndDeletedFalse(farmCrop.getFarmId()).orElse(null);
                    cropName = farmCrop.getCropName();
                }
            }

            if (reporterFarm == null) {
                // Path B: fallback — use the user's first farm with coordinates
                List<Farm> userFarms = farmRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
                reporterFarm = userFarms.stream()
                        .filter(f -> f.getLatitude() != null && f.getLongitude() != null)
                        .findFirst()
                        .orElse(null);
            }

            if (reporterFarm == null || reporterFarm.getLatitude() == null) {
                log.warn("Cannot broadcast: no farm with coordinates found for user {}", userId);
                return;
            }

            // If we couldn't resolve crop name from FarmCrop, try to get it from
            // the user's active crops on this farm
            if (cropName == null) {
                List<FarmCrop> activeCrops = farmCropRepository
                        .findByFarmIdAndDeletedFalseOrderByCreatedAtDesc(reporterFarm.getId());
                cropName = activeCrops.stream()
                        .filter(fc -> "ACTIVE".equals(fc.getStatus()))
                        .findFirst()
                        .map(FarmCrop::getCropName)
                        .orElse(null);
            }

            if (cropName == null) {
                log.warn("Cannot broadcast: no active crop found for user {}", userId);
                return;
            }

            String village = reporterFarm.getVillage();
            String district = reporterFarm.getDistrict();
            String state = reporterFarm.getState();

            // 2. Deduplication: skip if same disease+district was alerted in last 24h
            if (district != null && diseaseAlertRepository
                    .existsByDiseaseNameAndDistrictAndCreatedAtAfter(
                            diseaseName, district,
                            Instant.now().minus(DEDUP_HOURS, ChronoUnit.HOURS))) {
                log.info("Disease alert deduplicated: {} in {} was already reported in last {}h",
                        diseaseName, district, DEDUP_HOURS);
                return;
            }

            // 3. Find all farms growing the same crop (excluding reporter)
            List<Farm> candidateFarms = farmRepository.findFarmsWithCropExcludingUser(cropName, userId);

            if (candidateFarms.isEmpty()) {
                log.info("No nearby farms found growing {} to alert", cropName);
                return;
            }

            // 4. Filter by Haversine distance ≤ 100km
            double reporterLat = reporterFarm.getLatitude().doubleValue();
            double reporterLon = reporterFarm.getLongitude().doubleValue();

            // Group by userId to avoid sending multiple alerts to the same farmer
            // (they may have multiple farms in range)
            Map<UUID, Double> nearbyFarmers = new LinkedHashMap<>();
            for (Farm farm : candidateFarms) {
                double distance = haversine(reporterLat, reporterLon,
                        farm.getLatitude().doubleValue(), farm.getLongitude().doubleValue());
                if (distance <= ALERT_RADIUS_KM) {
                    nearbyFarmers.merge(farm.getUserId(), distance, Math::min);
                }
            }

            if (nearbyFarmers.isEmpty()) {
                log.info("No farms within {}km radius for disease alert: {} on {}",
                        ALERT_RADIUS_KM, diseaseName, cropName);
                return;
            }

            // 5. Publish disease alert for each nearby farmer
            for (Map.Entry<UUID, Double> entry : nearbyFarmers.entrySet()) {
                DiseaseAlertEvent alertEvent = DiseaseAlertEvent.builder()
                        .targetUserId(entry.getKey().toString())
                        .reporterUserId(userId.toString())
                        .cropName(cropName)
                        .diseaseName(diseaseName)
                        .description(event.getDescription())
                        .treatment(event.getTreatment())
                        .prevention(event.getPrevention())
                        .reportedVillage(village != null ? village : "")
                        .reportedDistrict(district != null ? district : "")
                        .reportedState(state != null ? state : "")
                        .distanceKm(entry.getValue())
                        .build();

                kafkaTemplate.send(KafkaTopics.DISEASE_ALERT_TOPIC, entry.getKey().toString(), alertEvent);
            }

            log.info("Disease alert broadcasted: {} on {} → {} farmers within {}km of {}/{}",
                    diseaseName, cropName, nearbyFarmers.size(), ALERT_RADIUS_KM, village, district);

            // 6. Record the outbreak
            DiseaseAlert record = DiseaseAlert.builder()
                    .reporterUserId(userId)
                    .cropName(cropName)
                    .diseaseName(diseaseName)
                    .description(event.getDescription())
                    .treatment(event.getTreatment() != null ? String.join("|||", event.getTreatment()) : null)
                    .prevention(event.getPrevention() != null ? String.join("|||", event.getPrevention()) : null)
                    .latitude(reporterFarm.getLatitude())
                    .longitude(reporterFarm.getLongitude())
                    .village(village)
                    .district(district)
                    .state(state)
                    .radiusKm((int) ALERT_RADIUS_KM)
                    .farmersNotified(nearbyFarmers.size())
                    .build();
            diseaseAlertRepository.save(record);

        } catch (Exception e) {
            log.error("Failed to broadcast disease alert for analysisId={}: {}",
                    event.getAnalysisId(), e.getMessage(), e);
        }
    }

    /**
     * Haversine formula — same approach used in MandiLocatorService.
     */
    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 10.0) / 10.0;
    }
}
