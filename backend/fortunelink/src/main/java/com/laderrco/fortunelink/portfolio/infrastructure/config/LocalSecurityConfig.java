package com.laderrco.fortunelink.portfolio.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Profile("local")
@Configuration
@EnableWebSecurity
public class LocalSecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    System.out.println("DEBUG: LocalSecurityConfig is LOADED!");
    http.csrf(AbstractHttpConfigurer::disable) // Often needed for POST/PATCH testing
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/health").permitAll()
            .requestMatchers("/api/v1/portfolios/**").permitAll() // For testing
            .anyRequest().authenticated()
        );
    return http.build();
  }
}