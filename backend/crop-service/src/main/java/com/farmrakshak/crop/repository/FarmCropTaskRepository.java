package com.farmrakshak.crop.repository;

import com.farmrakshak.crop.entity.FarmCropTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FarmCropTaskRepository extends JpaRepository<FarmCropTask, UUID> {

    List<FarmCropTask> findByFarmCropIdOrderByDayNumberAsc(UUID farmCropId);

    List<FarmCropTask> findByFarmCropIdAndStatusOrderByDayNumberAsc(UUID farmCropId, String status);

    List<FarmCropTask> findByUserIdAndDueDateAndStatus(UUID userId, LocalDate dueDate, String status);

    @Query("SELECT t FROM FarmCropTask t WHERE t.dueDate = :date AND t.status = 'PENDING'")
    List<FarmCropTask> findAllPendingByDueDate(@Param("date") LocalDate date);

    @Query("SELECT t FROM FarmCropTask t WHERE t.userId = :userId AND t.dueDate <= :date AND t.status = 'PENDING' ORDER BY t.dueDate ASC")
    List<FarmCropTask> findPendingUpToDate(@Param("userId") UUID userId, @Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM FarmCropTask t WHERE t.farmCropId = :farmCropId")
    void deleteAllByFarmCropId(@Param("farmCropId") UUID farmCropId);

    long countByFarmCropIdAndStatus(UUID farmCropId, String status);
}
