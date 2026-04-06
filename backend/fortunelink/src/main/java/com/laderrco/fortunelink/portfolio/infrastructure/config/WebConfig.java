package com.laderrco.fortunelink.portfolio.infrastructure.config;

import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUserResolver;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;
import com.laderrco.fortunelink.portfolio.infrastructure.config.serialization.CaseInsensitiveEnumConverterFactory;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

  private final RateLimitInterceptor rateLimitInterceptor;
  private final AuthenticatedUserResolver authenticatedUserResolver;

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
    registry.addInterceptor(Objects.requireNonNull(rateLimitInterceptor))
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/actuator/**",
            "/swagger-ui/**",
            "/api-docs/**");
  }
}