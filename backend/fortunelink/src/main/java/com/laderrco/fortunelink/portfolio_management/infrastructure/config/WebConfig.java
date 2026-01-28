package com.laderrco.fortunelink.portfolio_management.infrastructure.config;

import java.util.Objects;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.NonNull;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @NonNull
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    /**
     * Register rate limit interceptor for API endpoints.
     * 
     * Applied to: /api/**
     * Excluded: /actuator/** (monitoring endpoints)
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(Objects.requireNonNull(rateLimitInterceptor))
                .addPathPatterns("/api/**") // Apply to all API endpoints
                .excludePathPatterns(
                        "/actuator/**", // Don't rate limit health checks
                        "/swagger-ui/**", // Don't rate limit API docs
                        "/api-docs/**" // Don't rate limit OpenAPI spec
                );
    }

    /**
     * CORS configuration for frontend access.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:3000", // React dev server
                        "http://localhost:5173" // Vite dev server
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(
                        "X-Rate-Limit-Remaining", // Expose rate limit headers
                        "X-Rate-Limit-Retry-After-Seconds")
                .allowCredentials(true);
    }
}
