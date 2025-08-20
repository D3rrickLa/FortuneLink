package com.laderrco.fortunelink.shared;

public class ValidationUtils {
    public static void requireNonNulls(Object... values) {
        for (Object value : values) {
            if (value == null) { 
                throw new NullPointerException("Required value is null."); 
            }
        }
    }

    public static void requireNamedNonNull(Object... nameValuePairs) {
        if (nameValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Arguments must be in name-value pairs.");
        }

        for (int i = 0; i < nameValuePairs.length; i += 2) {
            String name = (String) nameValuePairs[i];
            Object value = nameValuePairs[i + 1];
            if (value == null) {
                throw new NullPointerException(name + " cannot be null.");
            }
        }
    }
}
