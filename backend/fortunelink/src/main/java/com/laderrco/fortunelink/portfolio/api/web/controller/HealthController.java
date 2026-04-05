package com.laderrco.fortunelink.portfolio.api.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HealthController {

  @GetMapping("/api/v1/health")
  public String healthCheck() {
    log.info(">>> Health check endpoint hit! <<<");
    return "Fortunelink API is UP";
  }
}