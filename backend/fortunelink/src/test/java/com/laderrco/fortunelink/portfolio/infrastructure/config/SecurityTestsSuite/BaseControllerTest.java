package com.laderrco.fortunelink.portfolio.infrastructure.config.SecurityTestsSuite;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Returns a RequestPostProcessor that injects a valid JWT principal.
     * Use this for standard authenticated requests.
     *
     * userId must be a valid UUID , it maps to the 'sub' claim your
     * AuthenticationUserService reads.
     */
    protected RequestPostProcessor authenticatedUser(UUID userId) {
        return jwt()
            .jwt(jwt -> jwt
                .subject(userId.toString())
                .claim("email", "test@fortunelink.com")
                .claim("role", "authenticated"));
    }

    protected RequestPostProcessor authenticatedUser(UUID userId, String... roles) {
        List<GrantedAuthority> authorities = Arrays.stream(roles)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        return jwt()
            .jwt(jwt -> jwt
                .subject(userId.toString())
                .claim("email", "test@fortunelink.com"))
            .authorities(authorities);
    }

    protected RequestPostProcessor adminUser(UUID userId) {
        return authenticatedUser(userId, "ROLE_ADMIN", "ROLE_USER");
    }
}