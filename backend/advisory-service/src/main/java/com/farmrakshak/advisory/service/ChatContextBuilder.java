package com.farmrakshak.advisory.service;

import com.farmrakshak.advisory.dto.CropContext;
import com.farmrakshak.advisory.dto.WeatherContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatContextBuilder {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${services.crop-url:http://crop-service}")
    private String cropServiceUrl;

    @Value("${services.weather-url:http://weather-service}")
    private String weatherServiceUrl;

    public String buildContext(UUID userId, double lat, double lon) {
        // Try farm-based crops first, fall back to legacy lifecycle crops
        List<CropContext> crops = fetchCrops(userId);
        if (crops.isEmpty()) {
            log.info("No farm crops found for chat, falling back to lifecycle crops");
            crops = fetchLegacyCrops(userId);
        }
        enrichWithDisease(userId, crops);
        WeatherContext weather = fetchWeather(lat, lon);

        StringBuilder ctx = new StringBuilder();

        // ── Farms & Crops ──
        ctx.append("═══ FARMER'S FARMS & CROPS ═══\n");
        if (crops.isEmpty()) {
            ctx.append("No active crops registered.\n\n");
        } else {
            // Group by farm
            Map<String, List<CropContext>> byFarm = new LinkedHashMap<>();
            for (CropContext c : crops) {
                String key = c.getFarmName() != null ? c.getFarmName() : "Unknown Farm";
                byFarm.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
            }

            int i = 1;
            for (Map.Entry<String, List<CropContext>> entry : byFarm.entrySet()) {
                CropContext first = entry.getValue().get(0);
                ctx.append(String.format("\n── Farm: %s ──\n", entry.getKey()));
                if (first.getVillage() != null || first.getDistrict() != null || first.getState() != null) {
                    ctx.append(String.format("   Location: %s%s%s\n",
                            first.getVillage() != null ? first.getVillage() : "",
                            first.getDistrict() != null ? ", " + first.getDistrict() : "",
                            first.getState() != null ? ", " + first.getState() : ""));
                }
                if (first.getFarmTotalArea() > 0) {
                    ctx.append(String.format("   Farm Size: %.1f %s | Soil: %s\n",
                            first.getFarmTotalArea(),
                            first.getAreaUnit() != null ? first.getAreaUnit() : "acres",
                            first.getSoilType() != null ? first.getSoilType() : "Unknown"));
                }

                for (CropContext c : entry.getValue()) {
                    ctx.append(String.format("%d. %s — Stage: %s (Day %d since sowing)\n",
                            i++, c.getCropName(), c.getCropStage(), c.getDaysSinceSowing()));
                    if (c.getAreaAllocated() > 0) {
                        ctx.append(String.format("   Area: %.1f %s\n", c.getAreaAllocated(),
                                first.getAreaUnit() != null ? first.getAreaUnit() : "acres"));
                    }
                    if (c.getSoilType() != null)
                        ctx.append("   Soil: ").append(c.getSoilType()).append("\n");
                    if (c.getIrrigationType() != null)
                        ctx.append("   Irrigation: ").append(c.getIrrigationType()).append("\n");

                    if (c.getDiseaseHistory() != null && !c.getDiseaseHistory().isEmpty()) {
                        ctx.append("   Recent diseases:\n");
                        for (CropContext.DiseaseRecord d : c.getDiseaseHistory()) {
                            ctx.append(String.format("   - %s (confidence %.0f%%)\n",
                                    d.getDiseaseName(), d.getConfidence() * 100));
                        }
                    }
                }
            }
            ctx.append("\n");
        }

        // ── Weather ──
        ctx.append("═══ CURRENT WEATHER ═══\n");
        if (weather != null) {
            ctx.append(String.format("Location: %s%s\n",
                    weather.getCityName() != null ? weather.getCityName() : "Unknown",
                    weather.getCountry() != null ? ", " + weather.getCountry() : ""));
            ctx.append(String.format("Temperature: %.1f°C (Feels like: %.1f°C, Min: %.1f°C, Max: %.1f°C)\n",
                    weather.getTemperature(), weather.getFeelsLike(),
                    weather.getTempMin(), weather.getTempMax()));
            ctx.append(String.format("Humidity: %.0f%% | Wind: %.1f km/h | Clouds: %d%%\n",
                    weather.getHumidity(), weather.getWindSpeed(), weather.getCloudCover()));
            ctx.append(String.format("Condition: %s | Visibility: %d km | Rain: %s\n",
                    weather.getCondition() != null ? weather.getCondition() : "Unknown",
                    weather.getVisibility(),
                    weather.isRainForecast() ? "Yes (" + weather.getRainVolume() + " mm)" : "No"));
            ctx.append(String.format("Pressure: %d hPa\n", weather.getPressure()));
            if (weather.getSunrise() > 0) {
                ctx.append(String.format("Sunrise: %s | Sunset: %s\n",
                        java.time.Instant.ofEpochSecond(weather.getSunrise()).atZone(java.time.ZoneId.systemDefault()).toLocalTime().toString(),
                        java.time.Instant.ofEpochSecond(weather.getSunset()).atZone(java.time.ZoneId.systemDefault()).toLocalTime().toString()));
            }
            if (weather.getAlerts() != null && !weather.getAlerts().isEmpty()) {
                ctx.append("\nFARMING WEATHER ALERTS:\n");
                for (String alert : weather.getAlerts()) {
                    ctx.append("  - ").append(alert).append("\n");
                }
            }
        } else {
            ctx.append("Weather data unavailable.\n");
        }

        return ctx.toString();
    }

    // ─────── inter-service calls — Farm-aware ───────

    @SuppressWarnings("unchecked")
    private List<CropContext> fetchCrops(UUID userId) {
        try {
            WebClient client = webClientBuilder.build();

            Map<?, ?> farmsResp = client.get()
                    .uri(cropServiceUrl + "/api/v1/farms/my")
                    .header("X-User-Id", userId.toString())
                    .retrieve().bodyToMono(Map.class).block();

            if (farmsResp == null || farmsResp.get("data") == null) return Collections.emptyList();

            List<Map<String, Object>> farms = (List<Map<String, Object>>) farmsResp.get("data");
            List<CropContext> allCrops = new ArrayList<>();

            for (Map<String, Object> farm : farms) {
                String farmId = str(farm, "id");
                String farmName = str(farm, "farmName");
                String village = str(farm, "village");
                String district = str(farm, "district");
                String state = str(farm, "state");
                double farmTotalArea = dbl(farm, "totalArea");
                String areaUnit = str(farm, "areaUnit");
                String soilType = str(farm, "soilType");

                try {
                    Map<?, ?> cropsResp = client.get()
                            .uri(cropServiceUrl + "/api/v1/farms/" + farmId + "/crops")
                            .header("X-User-Id", userId.toString())
                            .retrieve().bodyToMono(Map.class).block();

                    if (cropsResp == null || cropsResp.get("data") == null) continue;

                    List<Map<String, Object>> cropsData = (List<Map<String, Object>>) cropsResp.get("data");
                    for (Map<String, Object> c : cropsData) {
                        allCrops.add(CropContext.builder()
                                .cropId(str(c, "id"))
                                .cropName(str(c, "cropName"))
                                .cropStage(str(c, "cropStage"))
                                .daysSinceSowing(intVal(c, "daysSinceSowing"))
                                .totalGrowthDays(0)
                                .progressPercent(0)
                                .soilType(soilType)
                                .irrigationType(str(c, "irrigationType"))
                                .areaAllocated(dbl(c, "areaAllocated"))
                                .farmId(farmId)
                                .farmName(farmName)
                                .village(village)
                                .district(district)
                                .state(state)
                                .farmTotalArea(farmTotalArea)
                                .areaUnit(areaUnit != null ? areaUnit : "acres")
                                .diseaseHistory(new ArrayList<>())
                                .build());
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch crops for farm {}: {}", farmId, e.getMessage());
                }
            }

            return allCrops;
        } catch (Exception e) {
            log.warn("Failed to fetch farm crops: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private void enrichWithDisease(UUID userId, List<CropContext> crops) {
        try {
            Map<?, ?> resp = webClientBuilder.build().get()
                    .uri(cropServiceUrl + "/api/v1/crops/internal/disease-history?userId=" + userId)
                    .header("X-User-Id", userId.toString())
                    .retrieve().bodyToMono(Map.class).block();
            if (resp == null || resp.get("data") == null) return;

            List<Map<String, Object>> diseases = (List<Map<String, Object>>) resp.get("data");
            for (CropContext crop : crops) {
                List<CropContext.DiseaseRecord> records = diseases.stream()
                        .filter(d -> d.get("diseaseName") != null)
                        .filter(d -> crop.getCropId() != null && crop.getCropId().equals(str(d, "userCropId")))
                        .map(d -> CropContext.DiseaseRecord.builder()
                                .diseaseName(str(d, "diseaseName"))
                                .confidence(dbl(d, "confidence"))
                                .treatment(str(d, "treatment"))
                                .analyzedAt(str(d, "analyzedAt"))
                                .build())
                        .collect(Collectors.toList());
                crop.setDiseaseHistory(records);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch disease history: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private WeatherContext fetchWeather(double lat, double lon) {
        try {
            Map<?, ?> resp = webClientBuilder.build().get()
                    .uri(weatherServiceUrl + "/api/v1/weather?lat=" + lat + "&lon=" + lon)
                    .retrieve().bodyToMono(Map.class).block();
            if (resp == null || resp.get("data") == null) return null;

            Map<String, Object> data = (Map<String, Object>) resp.get("data");
            Object inner = data.get("data");
            Map<String, Object> wd;
            if (inner instanceof String) {
                wd = objectMapper.readValue((String) inner, new TypeReference<>() {});
            } else {
                wd = (Map<String, Object>) inner;
            }

            return WeatherContext.builder()
                    .temperature(dbl(wd, "temperature"))
                    .feelsLike(dbl(wd, "feelsLike"))
                    .tempMin(dbl(wd, "tempMin"))
                    .tempMax(dbl(wd, "tempMax"))
                    .humidity(dbl(wd, "humidity"))
                    .pressure(intVal(wd, "pressure"))
                    .windSpeed(dbl(wd, "windSpeed"))
                    .windDeg(intVal(wd, "windDeg"))
                    .condition(str(wd, "description"))
                    .mainWeather(str(wd, "mainWeather"))
                    .cloudCover(intVal(wd, "cloudCover"))
                    .visibility(intVal(wd, "visibility"))
                    .rainVolume(dbl(wd, "rainVolume"))
                    .rainForecast(Boolean.TRUE.equals(wd.get("rainForecast")))
                    .cityName(str(wd, "cityName"))
                    .country(str(wd, "country"))
                    .sunrise(wd.containsKey("sunrise") && wd.get("sunrise") instanceof Number ? ((Number) wd.get("sunrise")).longValue() : 0)
                    .sunset(wd.containsKey("sunset") && wd.get("sunset") instanceof Number ? ((Number) wd.get("sunset")).longValue() : 0)
                    .alerts(wd.containsKey("alerts") ? (List<String>) wd.get("alerts") : Collections.emptyList())
                    .lat(lat).lon(lon)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch weather: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fallback: fetch crops from the legacy lifecycle API (no farm context).
     */
    @SuppressWarnings("unchecked")
    private List<CropContext> fetchLegacyCrops(UUID userId) {
        try {
            Map<?, ?> resp = webClientBuilder.build().get()
                    .uri(cropServiceUrl + "/api/v1/crops/lifecycle/my-crops")
                    .header("X-User-Id", userId.toString())
                    .retrieve().bodyToMono(Map.class).block();
            if (resp == null || resp.get("data") == null) return Collections.emptyList();

            List<Map<String, Object>> list = (List<Map<String, Object>>) resp.get("data");
            return list.stream().map(m -> CropContext.builder()
                    .cropId(str(m, "userCropId"))
                    .cropName(str(m, "cropName"))
                    .cropStage(str(m, "currentStageName"))
                    .daysSinceSowing(intVal(m, "daysSinceSowing"))
                    .totalGrowthDays(intVal(m, "totalGrowthDays"))
                    .progressPercent(dbl(m, "progressPercent"))
                    .soilType(str(m, "soilType"))
                    .irrigationType(str(m, "irrigationType"))
                    .diseaseHistory(new ArrayList<>())
                    .build()
            ).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch legacy crops: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String str(Map<?, ?> m, String k) { Object v = m.get(k); return v != null ? v.toString() : null; }
    private int intVal(Map<?, ?> m, String k) { Object v = m.get(k); return v instanceof Number ? ((Number) v).intValue() : 0; }
    private double dbl(Map<?, ?> m, String k) { Object v = m.get(k); return v instanceof Number ? ((Number) v).doubleValue() : 0.0; }
}
