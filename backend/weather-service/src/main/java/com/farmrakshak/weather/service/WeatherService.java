package com.farmrakshak.weather.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final StringRedisTemplate redisTemplate;

    @Value("${weather.api.key:demo}")
    private String apiKey;

    @Value("${weather.api.url:https://api.openweathermap.org/data/2.5/weather}")
    private String apiUrl;

    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getWeather(double lat, double lon) {
        // Round coords to 2 decimal places for consistent cache keys
        String cacheKey = String.format("weather:%.2f:%.2f", lat, lon);

        // Check Redis cache first
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Weather cache hit: {}", cacheKey);
            return Map.of("source", "cache", "data", cached);
        }

        // Call real OpenWeatherMap API
        Map<String, Object> weatherData = fetchFromOpenWeatherMap(lat, lon);

        // Cache the result
        try {
            String json = objectMapper.writeValueAsString(weatherData);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
            log.info("Weather cached: {} -> {}°C, {}", cacheKey,
                    weatherData.get("temperature"), weatherData.get("description"));
        } catch (Exception e) {
            log.warn("Failed to cache weather data", e);
        }

        return Map.of("source", "api", "data", weatherData);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchFromOpenWeatherMap(double lat, double lon) {
        try {
            String url = String.format("%s?lat=%s&lon=%s&appid=%s&units=metric",
                    apiUrl, lat, lon, apiKey);

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                log.error("Null response from OpenWeatherMap");
                return fallbackWeather();
            }

            // Parse the OpenWeatherMap response
            Map<String, Object> main = (Map<String, Object>) response.get("main");
            Map<String, Object> wind = (Map<String, Object>) response.get("wind");
            Map<String, Object> sys = (Map<String, Object>) response.get("sys");
            Map<String, Object> clouds = (Map<String, Object>) response.get("clouds");
            List<Map<String, Object>> weatherList = (List<Map<String, Object>>) response.get("weather");

            double temp = main != null ? toDouble(main.get("temp")) : 0;
            double feelsLike = main != null ? toDouble(main.get("feels_like")) : 0;
            double tempMin = main != null ? toDouble(main.get("temp_min")) : 0;
            double tempMax = main != null ? toDouble(main.get("temp_max")) : 0;
            int humidity = main != null ? toInt(main.get("humidity")) : 0;
            int pressure = main != null ? toInt(main.get("pressure")) : 0;

            double windSpeed = wind != null ? toDouble(wind.get("speed")) * 3.6 : 0; // m/s -> km/h
            int windDeg = wind != null ? toInt(wind.get("deg")) : 0;

            String description = "Unknown";
            String icon = "01d";
            String mainWeather = "Clear";
            if (weatherList != null && !weatherList.isEmpty()) {
                description = (String) weatherList.get(0).getOrDefault("description", "Unknown");
                icon = (String) weatherList.get(0).getOrDefault("icon", "01d");
                mainWeather = (String) weatherList.get(0).getOrDefault("main", "Clear");
            }

            int cloudCover = clouds != null ? toInt(clouds.get("all")) : 0;

            // Check for rain
            Map<String, Object> rain = (Map<String, Object>) response.get("rain");
            double rainVolume = 0;
            if (rain != null) {
                rainVolume = rain.containsKey("1h") ? toDouble(rain.get("1h")) :
                             rain.containsKey("3h") ? toDouble(rain.get("3h")) : 0;
            }

            boolean rainForecast = rainVolume > 0 || mainWeather.equalsIgnoreCase("Rain")
                    || mainWeather.equalsIgnoreCase("Drizzle") || mainWeather.equalsIgnoreCase("Thunderstorm");

            // Visibility
            int visibility = response.containsKey("visibility") ? toInt(response.get("visibility")) / 1000 : 10; // km

            // City name
            String cityName = (String) response.getOrDefault("name", "Unknown");

            // Country
            String country = sys != null ? (String) sys.getOrDefault("country", "") : "";

            // Sunrise/Sunset
            long sunrise = sys != null ? toLong(sys.get("sunrise")) : 0;
            long sunset = sys != null ? toLong(sys.get("sunset")) : 0;

            // Generate farming alerts
            List<String> alerts = generateFarmingAlerts(temp, humidity, windSpeed, rainForecast, cloudCover);

            // Build the response
            Map<String, Object> weatherData = new LinkedHashMap<>();
            weatherData.put("temperature", Math.round(temp * 10.0) / 10.0);
            weatherData.put("feelsLike", Math.round(feelsLike * 10.0) / 10.0);
            weatherData.put("tempMin", Math.round(tempMin * 10.0) / 10.0);
            weatherData.put("tempMax", Math.round(tempMax * 10.0) / 10.0);
            weatherData.put("humidity", humidity);
            weatherData.put("pressure", pressure);
            weatherData.put("windSpeed", Math.round(windSpeed * 10.0) / 10.0);
            weatherData.put("windDeg", windDeg);
            weatherData.put("description", description);
            weatherData.put("mainWeather", mainWeather);
            weatherData.put("icon", icon);
            weatherData.put("cloudCover", cloudCover);
            weatherData.put("visibility", visibility);
            weatherData.put("rainVolume", rainVolume);
            weatherData.put("rainForecast", rainForecast);
            weatherData.put("cityName", cityName);
            weatherData.put("country", country);
            weatherData.put("sunrise", sunrise);
            weatherData.put("sunset", sunset);
            weatherData.put("alerts", alerts);

            log.info("Weather fetched from OpenWeatherMap: city={}, temp={}°C, humidity={}%, desc={}",
                    cityName, temp, humidity, description);

            return weatherData;

        } catch (Exception e) {
            log.error("Failed to fetch weather from OpenWeatherMap: {}", e.getMessage());
            return fallbackWeather();
        }
    }

    private List<String> generateFarmingAlerts(double temp, int humidity, double windSpeed,
                                                boolean rainForecast, int cloudCover) {
        List<String> alerts = new ArrayList<>();

        if (humidity > 85) alerts.add("🍄 High humidity (" + humidity + "%) — fungal disease risk is elevated. Monitor crops closely.");
        if (humidity < 30) alerts.add("🏜️ Very low humidity (" + humidity + "%) — crops may experience water stress. Increase irrigation.");
        if (temp > 38) alerts.add("🌡️ Extreme heat (" + temp + "°C) — risk of heat stress. Ensure adequate watering and shading.");
        if (temp > 35) alerts.add("☀️ High temperature (" + temp + "°C) — avoid spraying pesticides during peak heat.");
        if (temp < 5) alerts.add("❄️ Frost risk (" + temp + "°C) — protect tender crops with covers or mulch.");
        if (windSpeed > 40) alerts.add("💨 Strong winds (" + Math.round(windSpeed) + " km/h) — avoid spraying. Secure structures and supports.");
        if (windSpeed > 25) alerts.add("🌬️ Moderate winds (" + Math.round(windSpeed) + " km/h) — not ideal for pesticide/fertilizer application.");
        if (rainForecast) alerts.add("🌧️ Rain expected — postpone spraying and fertilizer application. Consider drainage.");

        return alerts;
    }

    private Map<String, Object> fallbackWeather() {
        log.warn("Using fallback weather data");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("temperature", 28.0);
        data.put("feelsLike", 30.0);
        data.put("tempMin", 25.0);
        data.put("tempMax", 32.0);
        data.put("humidity", 65);
        data.put("pressure", 1013);
        data.put("windSpeed", 12.0);
        data.put("windDeg", 180);
        data.put("description", "Data temporarily unavailable");
        data.put("mainWeather", "Clear");
        data.put("icon", "01d");
        data.put("cloudCover", 20);
        data.put("visibility", 10);
        data.put("rainVolume", 0);
        data.put("rainForecast", false);
        data.put("cityName", "Unknown");
        data.put("country", "");
        data.put("sunrise", 0);
        data.put("sunset", 0);
        data.put("alerts", List.of("⚠️ Weather data temporarily unavailable. Using defaults."));
        return data;
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0; }
    }

    private int toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }

    private long toLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return 0; }
    }
}
