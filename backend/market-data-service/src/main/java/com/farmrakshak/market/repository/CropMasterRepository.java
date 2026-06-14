package com.farmrakshak.market.repository;

import com.farmrakshak.market.entity.CropMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CropMasterRepository extends JpaRepository<CropMasterEntity, UUID> {
    Optional<CropMasterEntity> findByCropNameIgnoreCase(String cropName);

    @Query(value = "SELECT * FROM crops_master WHERE LOWER(crop_name) = LOWER(:q) " +
            "OR LOWER(:q) = ANY(SELECT LOWER(unnest(local_names))) " +
            "OR LOWER(:q) = ANY(SELECT LOWER(unnest(synonyms)))", nativeQuery = true)
    List<CropMasterEntity> searchByCropNameOrSynonym(@Param("q") String query);

    @Query(value = "SELECT * FROM crops_master WHERE LOWER(crop_name) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR EXISTS (SELECT 1 FROM unnest(local_names) ln WHERE LOWER(ln) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "OR EXISTS (SELECT 1 FROM unnest(synonyms) syn WHERE LOWER(syn) LIKE LOWER(CONCAT('%', :q, '%')))",
            nativeQuery = true)
    List<CropMasterEntity> fuzzySearch(@Param("q") String query);
}
