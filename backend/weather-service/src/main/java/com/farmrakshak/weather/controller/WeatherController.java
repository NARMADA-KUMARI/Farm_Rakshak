package com.farmrakshak.weather.controller;

import com.farmrakshak.shared.dto.ApiResponse;
import com.farmrakshak.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeather(
            @RequestParam double lat, @RequestParam double lon) {
        return ResponseEntity.ok(ApiResponse.success(weatherService.getWeather(lat, lon)));
    }
}
