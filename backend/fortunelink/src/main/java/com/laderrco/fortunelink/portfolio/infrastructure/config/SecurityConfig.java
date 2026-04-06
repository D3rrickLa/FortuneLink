package com.laderrco.fortunelink.portfolio.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Profile("!local & !test")
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  private final JwtAuthEntryPoint jwtAuthEntryPoint;
  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String issuerUri;

  SecurityConfig(JwtAuthEntryPoint jwtAuthEntryPoint) {
    this.jwtAuthEntryPoint = jwtAuthEntryPoint;
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    // withIssuerLocation() will:
    // 1. Call {issuerUri}/.well-known/openid-configuration
    // 2. Discover the JWK Set URI
    // 3. Fetch public keys
    // 4. Configure the decoder
    return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable).sessionManagement(
        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers("api/v1/public/**", "/actuator/health").permitAll()
                .anyRequest().authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()).authenticationEntryPoint(jwtAuthEntryPoint))
        .build();
  }
}