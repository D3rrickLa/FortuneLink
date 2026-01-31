package com.laderrco.fortunelink.portfolio_management.infrastructure.config;

import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.laderrco.fortunelink.portfolio_management.infrastructure.config.security.AuthenticatedUserResolver;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authenticatedUserResolver);
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
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(
                        "X-Rate-Limit-Remaining", // Expose rate limit headers
                        "X-Rate-Limit-Retry-After-Seconds")
                .allowCredentials(true);
    }
}
