package com.farmrakshak.weather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.farmrakshak.weather", "com.farmrakshak.shared"})
@EnableDiscoveryClient
public class WeatherServiceApplication {
    public static void main(String[] args) { SpringApplication.run(WeatherServiceApplication.class, args); }
}
