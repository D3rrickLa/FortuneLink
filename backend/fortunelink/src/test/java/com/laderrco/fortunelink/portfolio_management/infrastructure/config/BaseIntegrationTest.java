package com.laderrco.fortunelink.portfolio_management.infrastructure.config;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:alpine"));

    @BeforeAll
    static void setupDb() throws Exception {
        // Force start the container if it hasn't started yet
        postgres.start();
        // We create the missing "auth" piece before Flyway triggers
        try (var conn = postgres.createConnection("")) {
            var st = conn.createStatement();
            st.execute("CREATE SCHEMA IF NOT EXISTS auth;");
            st.execute("CREATE TABLE IF NOT EXISTS auth.users (id UUID PRIMARY KEY, email VARCHAR(255));");
        }
    }
}