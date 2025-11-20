package com.laderrco.fortunelink.shared.valueobjects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;


public class GenericIdTest {
    public record InnerGenericIdTest(UUID testUuid) implements GenericId {
    
        public static InnerGenericIdTest randomId() {
            return GenericId.random(InnerGenericIdTest::new);
        }
    }

    @Test
    public void testRandom() { 
        assertTrue(GenericId.random(InnerGenericIdTest::new).getClass().equals(InnerGenericIdTest.class));
    }

    @Test 
    public void testValidate() {
        assertThrows(NullPointerException.class, () -> GenericId.validate(null));
        assertNotNull(GenericId.validate(UUID.randomUUID()));
    }
}
