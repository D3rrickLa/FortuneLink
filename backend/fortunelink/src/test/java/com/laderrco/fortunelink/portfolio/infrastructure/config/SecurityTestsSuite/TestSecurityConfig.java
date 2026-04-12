package com.laderrco.fortunelink.portfolio.infrastructure.config.SecurityTestsSuite;

import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@TestConfiguration
public class TestSecurityConfig {

  @Bean
  public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/public/**").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
    return http.build();
  }

  /**
   * Provides a mock JwtDecoder so the context loads without hitting Supabase.
   * This bean is ONLY registered when this config is imported.
   */
  @Bean
  @Primary
  public JwtDecoder jwtDecoder() {
    return token -> {
      // Return a minimal valid Jwt for any token string
      return Jwt.withTokenValue(token)
          .header("alg", "RS256")
          .subject(UUID.randomUUID().toString())
          .claim("email", "test@fortunelink.com")
          .issuedAt(Instant.now())
          .expiresAt(Instant.now().plusSeconds(3600))
          .build();
    };
  }
}