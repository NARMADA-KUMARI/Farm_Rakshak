package com.farmrakshak.crop.service;

import com.farmrakshak.crop.entity.CropAnalysis;
import com.farmrakshak.crop.entity.CropUpload;
import com.farmrakshak.crop.entity.Farm;
import com.farmrakshak.crop.entity.FarmCrop;
import com.farmrakshak.crop.repository.CropAnalysisRepository;
import com.farmrakshak.crop.repository.CropUploadRepository;
import com.farmrakshak.crop.repository.FarmCropRepository;
import com.farmrakshak.crop.repository.FarmRepository;
import com.farmrakshak.shared.constants.KafkaTopics;
import com.farmrakshak.shared.event.AnalysisResultEvent;
import com.farmrakshak.shared.event.CropAnalysisEvent;
import com.farmrakshak.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CropService {

    private final CropUploadRepository uploadRepository;
    private final CropAnalysisRepository analysisRepository;
    private final ImageStorageService imageStorageService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final DiseaseAlertService diseaseAlertService;
    private final FarmCropRepository farmCropRepository;
    private final FarmRepository farmRepository;

    @Transactional
    public Map<String, Object> uploadAndAnalyze(MultipartFile file, UUID userId, UUID userCropId) {
        String objectName = imageStorageService.uploadImage(file, userId.toString());

        CropUpload upload = CropUpload.builder()
                .userId(userId)
                .userCropId(userCropId)
                .imageUrl(objectName)
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .status("PROCESSING")
                .build();
        upload = uploadRepository.save(upload);

        // Publish Kafka event for async AI analysis
        String fullImageUrl = imageStorageService.getPresignedUrl(objectName);
        CropAnalysisEvent event = CropAnalysisEvent.builder()
                .analysisId(upload.getId().toString())
                .imageUrl(fullImageUrl)
                .userId(userId.toString())
                .build();

        try {
            kafkaTemplate.send(KafkaTopics.CROP_ANALYSIS_TOPIC, userId.toString(), event);
            log.info("Published crop analysis event: uploadId={}", upload.getId());
        } catch (Exception e) {
            log.error("Failed to publish Kafka event, marking for retry", e);
            upload.setStatus("PENDING_RETRY");
            uploadRepository.save(upload);
        }

        return Map.of(
                "uploadId", upload.getId().toString(),
                "status", upload.getStatus(),
                "imageUrl", objectName
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalysisResult(UUID uploadId) {
        CropUpload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("CropUpload", "id", uploadId.toString()));

        Map<String, Object> result = new HashMap<>();
        result.put("uploadId", upload.getId().toString());
        result.put("status", upload.getStatus());
        result.put("imageUrl", imageStorageService.getPresignedUrl(upload.getImageUrl()));
        result.put("createdAt", upload.getCreatedAt().toString());

        analysisRepository.findByUploadId(uploadId).ifPresent(analysis -> {
            result.put("cropName", analysis.getCropName());
            result.put("diseaseName", analysis.getDiseaseName());
            result.put("confidence", analysis.getConfidence());
            result.put("description", analysis.getDescription());
            result.put("treatment", analysis.getTreatment());
            result.put("prevention", analysis.getPrevention());
            result.put("analyzedAt", analysis.getAnalyzedAt() != null ? analysis.getAnalyzedAt().toString() : null);

            if (analysis.getFarmCropId() != null) {
                result.put("farmCropId", analysis.getFarmCropId().toString());
                // Enrich with farm context
                enrichWithFarmContext(result, analysis.getFarmCropId());
            }
        });

        return result;
    }

    /**
     * Returns all matching active FarmCrops for the detected crop name.
     * Used when multiple farms have the same crop and the user needs to choose.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMatchingFarmCrops(UUID uploadId, UUID userId) {
        CropAnalysis analysis = analysisRepository.findByUploadId(uploadId).orElse(null);
        if (analysis == null || analysis.getCropName() == null) {
            return Collections.emptyList();
        }

        String detectedCrop = analysis.getCropName();
        List<Map<String, Object>> matches = new ArrayList<>();

        var farms = farmRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
        for (Farm farm : farms) {
            List<FarmCrop> crops = farmCropRepository
                    .findByFarmIdAndDeletedFalseOrderByCreatedAtDesc(farm.getId());
            for (FarmCrop fc : crops) {
                if ("ACTIVE".equals(fc.getStatus())
                        && fc.getCropName() != null
                        && cropNameMatches(fc.getCropName(), detectedCrop)) {
                    Map<String, Object> match = new LinkedHashMap<>();
                    match.put("farmCropId", fc.getId().toString());
                    match.put("cropName", fc.getCropName());
                    match.put("variety", fc.getVariety());
                    match.put("cropStage", fc.getCropStage());
                    match.put("farmId", farm.getId().toString());
                    match.put("farmName", farm.getFarmName());
                    match.put("village", farm.getVillage());
                    match.put("district", farm.getDistrict());
                    matches.add(match);
                }
            }
        }
        return matches;
    }

    /**
     * Manually links an analysis to a specific FarmCrop (user selected).
     */
    @Transactional
    public void linkAnalysisToFarmCrop(UUID uploadId, UUID farmCropId, UUID userId) {
        CropAnalysis analysis = analysisRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("CropAnalysis", "uploadId", uploadId.toString()));

        // Verify the farmCrop belongs to the user
        FarmCrop farmCrop = farmCropRepository.findByIdAndDeletedFalse(farmCropId)
                .orElseThrow(() -> new ResourceNotFoundException("FarmCrop", "id", farmCropId.toString()));
        Farm farm = farmRepository.findByIdAndDeletedFalse(farmCrop.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmCrop.getFarmId().toString()));
        if (!farm.getUserId().equals(userId)) {
            throw new IllegalArgumentException("FarmCrop does not belong to user");
        }

        analysis.setFarmCropId(farmCropId);
        analysisRepository.save(analysis);

        // Also update the upload's userCropId
        CropUpload upload = uploadRepository.findById(uploadId).orElse(null);
        if (upload != null) {
            upload.setUserCropId(farmCropId);
            uploadRepository.save(upload);
        }

        log.info("Manually linked analysis {} to farmCrop {} (farm: {})",
                uploadId, farmCropId, farm.getFarmName());
    }

    @Transactional
    public void processAnalysisResult(AnalysisResultEvent event) {
        UUID uploadId = UUID.fromString(event.getAnalysisId());
        CropUpload upload = uploadRepository.findById(uploadId).orElse(null);
        if (upload == null) {
            log.warn("Upload not found for analysis result: {}", event.getAnalysisId());
            return;
        }

        upload.setStatus(event.getStatus());
        uploadRepository.save(upload);

        // Smart auto-link: only auto-link if EXACTLY 1 match
        UUID matchedFarmCropId = null;
        String detectedCrop = event.getCropName();

        if (detectedCrop != null && upload.getUserId() != null) {
            matchedFarmCropId = autoLinkToFarmCrop(upload.getUserId(), detectedCrop, upload);
        }

        CropAnalysis analysis = CropAnalysis.builder()
                .uploadId(uploadId)
                .cropName(detectedCrop)
                .farmCropId(matchedFarmCropId)
                .diseaseName(event.getDiseaseName())
                .confidence(event.getConfidence() != null ? BigDecimal.valueOf(event.getConfidence()) : null)
                .description(event.getDescription())
                .treatment(event.getTreatment() != null ? String.join("|||", event.getTreatment()) : null)
                .prevention(event.getPrevention() != null ? String.join("|||", event.getPrevention()) : null)
                .analyzedAt(Instant.now())
                .build();
        analysisRepository.save(analysis);

        log.info("Analysis result stored: uploadId={}, crop={}, disease={}, farmCropId={}",
                uploadId, detectedCrop, event.getDiseaseName(), matchedFarmCropId);

        // Broadcast disease alert to nearby farmers (async, non-blocking)
        if (event.getDiseaseName() != null
                && !"Healthy".equalsIgnoreCase(event.getDiseaseName())
                && "COMPLETED".equals(event.getStatus())) {
            diseaseAlertService.broadcastDiseaseAlert(event);
        }
    }

    /**
     * Smart auto-link: only auto-links if EXACTLY 1 active FarmCrop matches the detected crop.
     * If multiple farms have the same crop, returns null so the user can manually choose.
     */
    private UUID autoLinkToFarmCrop(UUID userId, String detectedCropName, CropUpload upload) {
        try {
            List<FarmCrop> allMatches = new ArrayList<>();
            var farms = farmRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
            for (var farm : farms) {
                List<FarmCrop> crops = farmCropRepository
                        .findByFarmIdAndDeletedFalseOrderByCreatedAtDesc(farm.getId());
                for (FarmCrop fc : crops) {
                    if ("ACTIVE".equals(fc.getStatus())
                            && fc.getCropName() != null
                            && cropNameMatches(fc.getCropName(), detectedCropName)) {
                        allMatches.add(fc);
                    }
                }
            }

            if (allMatches.size() == 1) {
                FarmCrop match = allMatches.get(0);
                log.info("Auto-linked analysis to farmCrop: {} ({})", match.getId(), match.getCropName());
                if (upload.getUserCropId() == null) {
                    upload.setUserCropId(match.getId());
                    uploadRepository.save(upload);
                }
                return match.getId();
            } else if (allMatches.size() > 1) {
                log.info("Multiple farm crops match '{}' ({}), user must select manually",
                        detectedCropName, allMatches.size());
                return null;
            }

            log.info("No matching farm crop found for detected crop: {}", detectedCropName);
        } catch (Exception e) {
            log.warn("Failed to auto-link farm crop: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Fuzzy crop name matcher.
     * Handles AI returning "Rice (Oryza sativa)" while farm stores "Rice".
     * Checks: exact match, contains, or starts-with.
     */
    private boolean cropNameMatches(String farmCropName, String detectedCropName) {
        if (farmCropName == null || detectedCropName == null) return false;
        String farm = farmCropName.trim().toLowerCase();
        String detected = detectedCropName.trim().toLowerCase();

        // Exact match
        if (farm.equals(detected)) return true;
        // Farm name contained in detected name ("rice" in "rice (oryza sativa)")
        if (detected.contains(farm)) return true;
        // Detected name contained in farm name
        if (farm.contains(detected)) return true;
        // First word match ("rice" matches "rice (oryza sativa)")
        String detectedFirst = detected.split("[\\s(]+")[0];
        String farmFirst = farm.split("[\\s(]+")[0];
        if (farmFirst.equals(detectedFirst) && farmFirst.length() >= 3) return true;

        return false;
    }

    /**
     * Enriches analysis result with farm context (farm name, village, crop stage).
     */
    private void enrichWithFarmContext(Map<String, Object> result, UUID farmCropId) {
        try {
            FarmCrop fc = farmCropRepository.findByIdAndDeletedFalse(farmCropId).orElse(null);
            if (fc == null) return;
            result.put("linkedCropStage", fc.getCropStage());
            result.put("linkedVariety", fc.getVariety());

            Farm farm = farmRepository.findByIdAndDeletedFalse(fc.getFarmId()).orElse(null);
            if (farm != null) {
                result.put("linkedFarmId", farm.getId().toString());
                result.put("linkedFarmName", farm.getFarmName());
                result.put("linkedVillage", farm.getVillage());
                result.put("linkedDistrict", farm.getDistrict());
            }
        } catch (Exception e) {
            log.warn("Failed to enrich with farm context: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getHistory(UUID userId, Pageable pageable) {
        return uploadRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(upload -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("uploadId", upload.getId().toString());
                    item.put("status", upload.getStatus());
                    item.put("imageUrl", imageStorageService.getPresignedUrl(upload.getImageUrl()));
                    item.put("createdAt", upload.getCreatedAt().toString());
                    analysisRepository.findByUploadId(upload.getId()).ifPresent(a -> {
                        item.put("cropName", a.getCropName());
                        item.put("diseaseName", a.getDiseaseName());
                        item.put("confidence", a.getConfidence());
                    });
                    return item;
                });
    }
}
