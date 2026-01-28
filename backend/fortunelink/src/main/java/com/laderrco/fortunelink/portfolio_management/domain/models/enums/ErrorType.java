package com.laderrco.fortunelink.portfolio_management.domain.models.enums;

    public enum ErrorType {
        SYMBOL_NOT_FOUND,       // Invalid/unknown symbol
        SYMBOL_NOT_SUPPORTED,   // Valid symbol but not supported by provider
        DATA_UNAVAILABLE,       // Temporarily unavailable (market closed, API down)
        RATE_LIMIT_EXCEEDED,    // API rate limit hit
        PROVIDER_ERROR,         // General provider error (HTTP 500, etc.)
        NETWORK_ERROR,          // Connection timeout, DNS failure
        INVALID_RESPONSE        // Malformed response from provider
    }