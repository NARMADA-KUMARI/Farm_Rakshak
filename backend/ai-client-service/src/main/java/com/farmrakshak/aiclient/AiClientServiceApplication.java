package com.farmrakshak.aiclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.farmrakshak.aiclient", "com.farmrakshak.shared"})
@EnableDiscoveryClient
public class AiClientServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiClientServiceApplication.class, args);
    }
}
