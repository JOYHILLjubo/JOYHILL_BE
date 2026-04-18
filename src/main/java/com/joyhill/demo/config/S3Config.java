package com.joyhill.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        // EC2 IAM Role 방식 — 자격증명 자동 주입 (Access Key 불필요)
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
