package com.farmrakshak.advisory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WeatherContext {
    private double temperature;
    private double feelsLike;
    private double tempMin;
    private double tempMax;
    private double humidity;
    private int pressure;
    private double windSpeed;
    private int windDeg;
    private String condition;
    private String mainWeather;
    private int cloudCover;
    private int visibility;
    private double rainVolume;
    private boolean rainForecast;
    private String cityName;
    private String country;
    private long sunrise;
    private long sunset;
    private List<String> alerts;
    private double lat;
    private double lon;
}
