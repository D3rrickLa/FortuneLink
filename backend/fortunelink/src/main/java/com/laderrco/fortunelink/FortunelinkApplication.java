package com.laderrco.fortunelink;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
public class FortunelinkApplication implements CommandLineRunner {
    @Autowired
    private Environment env;

    public static void main(String[] args) {
        java.io.File file = new java.io.File(".env");
        System.out.println("Does .env exist at " + file.getAbsolutePath() + "? " + file.exists());
        SpringApplication.run(FortunelinkApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("DB_URL from Env: " + env.getProperty("DB_URL"));
    }

}
