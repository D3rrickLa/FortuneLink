package com.laderrco.fortunelink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FortunelinkApplication {
  static void main(String[] args) {
    SpringApplication.run(FortunelinkApplication.class, args);
  }
}
