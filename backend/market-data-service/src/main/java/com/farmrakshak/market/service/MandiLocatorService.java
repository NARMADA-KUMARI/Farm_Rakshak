package com.farmrakshak.market.service;

import com.farmrakshak.market.entity.Mandi;
import com.farmrakshak.market.dto.MandiResponse;
import com.farmrakshak.market.repository.MandiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Haversine-based nearest mandi finder.
 */
@Service
@RequiredArgsConstructor
public class MandiLocatorService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private final MandiRepository mandiRepository;

    /**
     * Find nearest N mandis to a given coordinate using Haversine formula.
     */
    public List<MandiResponse> findNearest(BigDecimal lat, BigDecimal lon, int limit) {
        if (lat == null || lon == null) {
            // Fallback: return first N mandis without distance
            return mandiRepository.findByIsActiveTrue().stream()
                    .limit(limit)
                    .map(m -> toResponse(m, 0))
                    .toList();
        }

        double userLat = lat.doubleValue();
        double userLon = lon.doubleValue();

        return mandiRepository.findAllWithCoordinates().stream()
                .map(m -> {
                    double distance = haversine(userLat, userLon,
                            m.getLatitude().doubleValue(), m.getLongitude().doubleValue());
                    return toResponse(m, distance);
                })
                .sorted(Comparator.comparingDouble(MandiResponse::getDistanceKm))
                .limit(limit)
                .toList();
    }

    /**
     * Haversine formula to calculate distance between two points on Earth.
     */
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 10.0) / 10.0;
    }

    private MandiResponse toResponse(Mandi m, double distance) {
        return MandiResponse.builder()
                .id(m.getId().toString())
                .name(m.getName())
                .state(m.getState())
                .district(m.getDistrict())
                .latitude(m.getLatitude())
                .longitude(m.getLongitude())
                .distanceKm(distance)
                .build();
    }
}
