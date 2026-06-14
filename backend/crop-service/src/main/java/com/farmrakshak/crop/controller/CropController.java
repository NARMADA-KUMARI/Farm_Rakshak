package com.farmrakshak.crop.controller;

import com.farmrakshak.crop.entity.CropAnalysis;
import com.farmrakshak.crop.entity.CropUpload;
import com.farmrakshak.crop.repository.CropAnalysisRepository;
import com.farmrakshak.crop.repository.CropUploadRepository;
import com.farmrakshak.crop.service.CropService;
import com.farmrakshak.shared.dto.ApiResponse;
import com.farmrakshak.shared.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/crops")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;
    private final CropUploadRepository uploadRepository;
    private final CropAnalysisRepository analysisRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userCropId", required = false) String userCropId,
            @RequestHeader("X-User-Id") String userId) {
        UUID uci = userCropId != null && !userCropId.isEmpty() ? UUID.fromString(userCropId) : null;
        Map<String, Object> result = cropService.uploadAndAnalyze(file, UUID.fromString(userId), uci);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(result, "Image uploaded, analysis in progress"));
    }

    @GetMapping("/analysis/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalysis(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(cropService.getAnalysisResult(id)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedResponse<Map<String, Object>>>> getHistory(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Map<String, Object>> result = cropService.getHistory(UUID.fromString(userId), PageRequest.of(page, Math.min(size, 50)));
        PagedResponse<Map<String, Object>> paged = PagedResponse.<Map<String, Object>>builder()
                .content(result.getContent()).page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages()).build();
        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    /**
     * Returns matching FarmCrops for the detected crop in the analysis.
     * Frontend calls this to show a farm selector when multiple farms grow the same crop.
     */
    @GetMapping("/analysis/{id}/matching-crops")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMatchingCrops(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        List<Map<String, Object>> matches = cropService.getMatchingFarmCrops(id, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(matches));
    }

    /**
     * Manually links an analysis result to a specific FarmCrop.
     * Called when user selects which farm this crop belongs to.
     */
    @PutMapping("/analysis/{id}/link-crop")
    public ResponseEntity<ApiResponse<Void>> linkCrop(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String userId) {
        UUID farmCropId = UUID.fromString(body.get("farmCropId"));
        cropService.linkAnalysisToFarmCrop(id, farmCropId, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(null, "Analysis linked to farm crop"));
    }

    /**
     * Internal endpoint for advisory-service — returns disease history for a user.
     */
    @GetMapping("/internal/disease-history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDiseaseHistory(
            @RequestParam("userId") String userId) {
        UUID uid = UUID.fromString(userId);
        List<CropUpload> uploads = uploadRepository.findByUserIdOrderByCreatedAtDesc(uid, PageRequest.of(0, 50)).getContent();

        List<Map<String, Object>> history = uploads.stream()
                .map(upload -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("uploadId", upload.getId().toString());
                    item.put("createdAt", upload.getCreatedAt().toString());
                    if (upload.getUserCropId() != null) {
                        item.put("userCropId", upload.getUserCropId().toString());
                    }
                    analysisRepository.findByUploadId(upload.getId()).ifPresent(a -> {
                        item.put("diseaseName", a.getDiseaseName());
                        item.put("cropName", a.getCropName());
                        item.put("confidence", a.getConfidence());
                        item.put("treatment", a.getTreatment());
                        item.put("prevention", a.getPrevention());
                        item.put("analyzedAt", a.getAnalyzedAt() != null ? a.getAnalyzedAt().toString() : null);
                    });
                    return item;
                })
                .filter(m -> m.containsKey("diseaseName"))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
