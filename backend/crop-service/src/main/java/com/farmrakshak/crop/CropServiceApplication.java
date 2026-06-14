package com.farmrakshak.crop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.farmrakshak.crop", "com.farmrakshak.shared"})
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class CropServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CropServiceApplication.class, args);
    }
}
