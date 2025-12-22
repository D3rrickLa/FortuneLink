package com.laderrco.fortunelink.portfolio_management.application.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ValidationResultTest {
    @Test
    void sucessPasses() {
        ValidationResult test = ValidationResult.success();
        assertEquals(true, test.isValid());
        assertEquals(Collections.emptyList(), test.errors());
    }
    
    @Test
    void failureFailsSuccessfully() {
        List<String> errors = List.of("Test 1", "Test 2", "Test 3");
        ValidationResult test = ValidationResult.failure(errors);
        assertEquals(false, test.isValid());
        assertEquals(errors, test.errors());
    }

    @Test
    void failureFailsSingleSuccessfully() {
        String error = "Error";
        ValidationResult test = ValidationResult.failure(error);
        assertEquals(false, test.isValid());
        assertEquals(List.of(error), test.errors());
    }
}
