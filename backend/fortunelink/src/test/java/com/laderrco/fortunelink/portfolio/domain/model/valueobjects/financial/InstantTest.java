package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

public class InstantTest {

	@Test
	void testInstantConstructor_Success() {
		Instant date = Instant.now();
		assertNotNull(date);
	}

	@Test
	void testStaticConstructor_Success() {
		Instant date = Instant.now().plus(Duration.ofDays(30));
		assertNotNull(date);
	}

	@Test
	void testMethods() {
		Instant time = Instant.now();
		Instant date = time;
		Instant otherDate = Instant.MAX;
		Instant otherDate2 =Instant.MIN;

		assertTrue(date.isBefore(otherDate));
		assertTrue(date.isAfter(otherDate2));

		assertEquals(time.toEpochMilli(), date.toEpochMilli());

	}
}
