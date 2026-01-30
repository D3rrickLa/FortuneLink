package com.laderrco.fortunelink.portfolio_management.infrastructure.test_env;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;

// Test config - active in CI/tests
@Profile("test")
@Configuration
@EnableWebSecurity
public class TestSecurityConfig {
    @Bean
    public JwtDecoder jwtDecoder() throws JOSEException {
        // Uses the in-memory JWKs we generate in TestAuthorizationServerConfig
        RSAKey rsaKey = TestAuthorizationServerConfig.generateRsa();
        return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }
}