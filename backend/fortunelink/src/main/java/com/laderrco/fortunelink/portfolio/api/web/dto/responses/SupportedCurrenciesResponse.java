package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "List of all currencies supported for live exchange rates")
public record SupportedCurrenciesResponse(List<String> currencies) {
}
