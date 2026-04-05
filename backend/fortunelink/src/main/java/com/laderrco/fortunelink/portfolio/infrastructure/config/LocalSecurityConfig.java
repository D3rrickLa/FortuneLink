package com.laderrco.fortunelink.portfolio.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Profile("local")
@Configuration
@EnableWebSecurity
public class LocalSecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    System.out.println("DEBUG: LocalSecurityConfig is LOADED!");
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            // Try specifically permitting the ID path to see if 401 persists
            .requestMatchers(HttpMethod.GET, "/api/v1/portfolios/{id}").permitAll()
            .requestMatchers("/api/v1/portfolios/**").authenticated()
            .anyRequest().permitAll())
        .httpBasic(basic -> basic.disable()) // Turn off the popup login
        .formLogin(form -> form.disable()) // Turn off the login page
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(Customizer.withDefaults()));
    return http.build();
  }
}