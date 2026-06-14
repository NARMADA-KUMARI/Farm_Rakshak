package com.farmrakshak.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.farmrakshak.blog", "com.farmrakshak.shared"})
@EnableDiscoveryClient
public class BlogServiceApplication {
    public static void main(String[] args) { SpringApplication.run(BlogServiceApplication.class, args); }
}
