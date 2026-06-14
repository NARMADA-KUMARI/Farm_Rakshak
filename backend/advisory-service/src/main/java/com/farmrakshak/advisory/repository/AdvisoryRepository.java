package com.farmrakshak.advisory.repository;

import com.farmrakshak.advisory.entity.Advisory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID;

public interface AdvisoryRepository extends JpaRepository<Advisory, UUID> {
    Page<Advisory> findByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT a FROM Advisory a WHERE a.deletedAt IS NULL AND " +
           "(:crop IS NULL OR a.crop = :crop) AND " +
           "(:season IS NULL OR a.season = :season) AND " +
           "(:language IS NULL OR a.language = :language)")
    Page<Advisory> findFiltered(String crop, String season, String language, Pageable pageable);
}
