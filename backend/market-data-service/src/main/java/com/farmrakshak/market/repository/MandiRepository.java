package com.farmrakshak.market.repository;

import com.farmrakshak.market.entity.Mandi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface MandiRepository extends JpaRepository<Mandi, UUID> {
    List<Mandi> findByIsActiveTrue();
    List<Mandi> findByStateIgnoreCase(String state);
    List<Mandi> findByDistrictIgnoreCase(String district);

    @Query("SELECT m FROM Mandi m WHERE m.isActive = true AND m.latitude IS NOT NULL AND m.longitude IS NOT NULL")
    List<Mandi> findAllWithCoordinates();
}
