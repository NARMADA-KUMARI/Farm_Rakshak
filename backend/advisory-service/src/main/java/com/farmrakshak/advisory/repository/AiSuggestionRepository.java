package com.farmrakshak.advisory.repository;

import com.farmrakshak.advisory.entity.AiSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {

    @Query("SELECT s FROM AiSuggestion s WHERE s.userId = :userId AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<AiSuggestion> findFreshByUserId(UUID userId, Instant now);
}
