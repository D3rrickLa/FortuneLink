package com.laderrco.fortunelink;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class FortunelinkApplicationTests {
  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @BeforeAll
  static void setup() throws Exception {
    postgres.start();
    try (var conn = postgres.createConnection("")) {
      var st = conn.createStatement();
      // Flyway will handle creating the 'accounts' table based on your script,
      // but we MUST create the 'auth' schema first for the foreign keys to work.
      st.execute("CREATE SCHEMA IF NOT EXISTS auth;");
      st.execute(
          "CREATE TABLE IF NOT EXISTS auth.users (id UUID PRIMARY KEY, email VARCHAR(255));");
    }
  }

  @Test
  void contextLoads() {
    // Now it will pass because Flyway runs against 'postgres'
    // and creates the columns Hibernate expects.
  }
}