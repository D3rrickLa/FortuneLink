package com.laderrco.fortunelink;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {
	static ApplicationModules modules;

	@BeforeAll
	static void setup() {
		modules = ApplicationModules.of(FortunelinkApplication.class);
	}

	@Test
	void verifiesModuleStructure() {
		modules.verify();
	}

	@Test
	@Disabled
	void documentModules() {
		new Documenter(modules).writeDocumentation();
	}
}