package com.laderrco.fortunelink.portfolio.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

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
    RestTemplate restTemplate = new RestTemplate();
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(5000); // 5 sec
    requestFactory.setReadTimeout(5000);
    restTemplate.setRequestFactory(requestFactory);
    return NimbusJwtDecoder.withIssuerLocation(issuerUri).restOperations(restTemplate).build();
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return (web) -> web.ignoring().requestMatchers("/api/v1/public/**")
        .requestMatchers("/actuator/health/**");
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/public/**").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/api-docs", "/docs").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth -> oauth
            .jwt(Customizer.withDefaults())
            .authenticationEntryPoint(jwtAuthEntryPoint));

    return http.build();
  }
}