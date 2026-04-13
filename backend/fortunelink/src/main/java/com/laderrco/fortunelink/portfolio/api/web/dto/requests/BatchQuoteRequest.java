package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body for the batch quote endpoint.
 * <p>
 * Limited to 20 symbols server-side. The controller enforces this independently of this annotation
 * so there are two layers of protection , annotation handles the Spring validation pipeline,
 * controller handles the FMP quota concern.
 */
public record BatchQuoteRequest(
    @Size(max = 20, message = "Batch quote requests are limited to 20 symbols") List<String> symbols) {
}
