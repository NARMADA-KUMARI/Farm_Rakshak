package com.farmrakshak.advisory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.farmrakshak.advisory", "com.farmrakshak.shared"})
@EnableDiscoveryClient
public class AdvisoryServiceApplication {
    public static void main(String[] args) { SpringApplication.run(AdvisoryServiceApplication.class, args); }
}
