package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Request body for the batch quote endpoint.
 *
 * Limited to 20 symbols server-side. The controller enforces this independently
 * of this annotation so there are two layers of protection — annotation handles
 * the Spring validation pipeline, controller handles the FMP quota concern.
 */
public record BatchQuoteRequest(
    @NotEmpty @Size(max = 20, message = "Batch quote requests are limited to 20 symbols") List<String> symbols) {
}
