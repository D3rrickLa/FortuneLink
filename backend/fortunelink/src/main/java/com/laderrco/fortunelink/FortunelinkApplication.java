package com.laderrco.fortunelink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FortunelinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(FortunelinkApplication.class, args);
    }
}
