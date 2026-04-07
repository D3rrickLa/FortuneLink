package com.laderrco.fortunelink.portfolio.infrastructure.config;

import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUserResolver;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;
import com.laderrco.fortunelink.portfolio.infrastructure.config.serialization.CaseInsensitiveEnumConverterFactory;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
  private final RateLimitInterceptor rateLimitInterceptor;
  private final AuthenticatedUserResolver authenticatedUserResolver;
  @Value("${fortunelink.cors.allowed-origins:http://localhost:3000}")
  private String[] allowedOrigins;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    // This tells Spring to use your custom logic for @AuthenticatedUser parameters
    resolvers.add(authenticatedUserResolver);
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    // Register the custom factory to handle lowercase/mixed-case Enums in URLs
    registry.addConverterFactory(new CaseInsensitiveEnumConverterFactory());
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(Objects.requireNonNull(rateLimitInterceptor)).addPathPatterns("/api/**")
        .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/api-docs/**");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**").allowedOrigins(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("Authorization", "Content-Type", "X-Request-ID")
        .exposedHeaders("X-Rate-Limit-Remaining", "X-Request-ID").allowCredentials(true)
        .maxAge(3600);
  }
}