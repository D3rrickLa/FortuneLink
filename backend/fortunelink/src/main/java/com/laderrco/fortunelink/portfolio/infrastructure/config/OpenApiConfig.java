package com.laderrco.fortunelink.portfolio.infrastructure.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
public class OpenApiConfig {
  @Bean
  public OpenAPI fortuneLinkOpenAPI() {
    return new OpenAPI().info(new Info().title("FortuneLink API").description(
                "REST API for FortuneLink - Personal Finance Management & FIRE Community Platform")
            .version("v0.0.3").contact(new Contact().name("D3rrickLa").email("support@fortunelink.com"))
            .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
        .servers(List.of(
            new Server().url("http://localhost:8080").description("Local development server"),
            new Server().url("https://api.fortunelink.com")
                .description("Production server (future)")));
  }
}