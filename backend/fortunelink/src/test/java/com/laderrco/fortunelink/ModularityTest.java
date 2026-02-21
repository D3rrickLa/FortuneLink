package com.laderrco.fortunelink;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    ApplicationModules modules = ApplicationModules.of(FortunelinkApplication.class).verify();

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void printModules() {
        modules.forEach(System.out::println);
    }

    @Test
    void verifyScanning() throws IOException {
        var basePackage = FortunelinkApplication.class.getPackageName();
        System.out.println("Base package: " + basePackage);

        var urls = Thread.currentThread().getContextClassLoader().getResources(basePackage.replace('.', '/'));
        while (urls.hasMoreElements()) {
            System.out.println(urls.nextElement());
        }
    }
}