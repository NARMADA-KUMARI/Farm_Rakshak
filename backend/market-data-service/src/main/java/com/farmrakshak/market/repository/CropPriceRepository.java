package com.farmrakshak.market.repository;

import com.farmrakshak.market.entity.CropPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CropPriceRepository extends JpaRepository<CropPrice, UUID> {

    List<CropPrice> findByCropNameIgnoreCaseAndPriceDateOrderByPriceModalDesc(String cropName, LocalDate date);

    @Query("SELECT cp FROM CropPrice cp WHERE LOWER(cp.cropName) = LOWER(:crop) " +
            "AND cp.priceDate BETWEEN :startDate AND :endDate ORDER BY cp.priceDate ASC")
    List<CropPrice> findTrendData(@Param("crop") String cropName,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    @Query("SELECT cp FROM CropPrice cp WHERE LOWER(cp.cropName) = LOWER(:crop) " +
            "AND cp.mandiId = :mandiId AND cp.priceDate BETWEEN :startDate AND :endDate " +
            "ORDER BY cp.priceDate ASC")
    List<CropPrice> findTrendDataForMandi(@Param("crop") String cropName,
                                           @Param("mandiId") UUID mandiId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Query("SELECT cp FROM CropPrice cp WHERE LOWER(cp.cropName) = LOWER(:crop) " +
            "AND cp.priceDate = :date")
    List<CropPrice> findByCropAndDate(@Param("crop") String cropName, @Param("date") LocalDate date);

    @Query("SELECT cp FROM CropPrice cp WHERE LOWER(cp.cropName) = LOWER(:crop) " +
            "AND cp.mandiId IN :mandiIds AND cp.priceDate = :date ORDER BY cp.priceModal DESC")
    List<CropPrice> findByCropAndMandisAndDate(@Param("crop") String cropName,
                                                @Param("mandiIds") List<UUID> mandiIds,
                                                @Param("date") LocalDate date);

    @Query("SELECT DISTINCT cp.cropName FROM CropPrice cp WHERE cp.priceDate = :date")
    List<String> findDistinctCropNamesByDate(@Param("date") LocalDate date);

    boolean existsByCropNameIgnoreCaseAndMandiIdAndPriceDate(String cropName, UUID mandiId, LocalDate priceDate);
}
