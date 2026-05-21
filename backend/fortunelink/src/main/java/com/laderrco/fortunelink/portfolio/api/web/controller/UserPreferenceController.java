package com.laderrco.fortunelink.portfolio.api.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.laderrco.fortunelink.portfolio.application.services.UserPreferencesService;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * Exposes user-level preferences at a dedicated path so callers have a clear
 * contract. Intentionally separate from any portfolio endpoint — preferences
 * live in the Auth bounded context, not Portfolio Management.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/preferences")
@Tag(name = "User Preferences", description = "Endpoints for managing authenticated user preferences")
public class UserPreferenceController {
  private final UserPreferencesService preferencesService;

  @Operation(summary = "Get base currency", description = "Returns the authenticated user's preferred reporting currency")
  @ApiResponse(responseCode = "200", description = "Currency preference retrieved successfully")
  @GetMapping("/currency")
  public ResponseEntity<CurrencyPreferenceResponse> getBaseCurrency(@AuthenticatedUser UserId userId) {
    Currency currency = preferencesService.getBaseCurrency(userId);

    return ResponseEntity.ok(new CurrencyPreferenceResponse(currency.getCode()));
  }

  @Operation(summary = "Update base currency", description = "Updates the authenticated user's preferred reporting currency")
  @ApiResponse(responseCode = "204", description = "Currency preference updated successfully")
  @PutMapping("/currency")
  public ResponseEntity<Void> updateBaseCurrency(@AuthenticatedUser UserId userId,
      @Valid @RequestBody UpdateCurrencyRequest request) {
    Currency currency = Currency.of(request.currency().toUpperCase());

    preferencesService.updateBaseCurrency(userId, currency);

    return ResponseEntity.noContent().build();
  }

  // DTOs

  @Schema(description = "Base currency preference response")
  public record CurrencyPreferenceResponse(
      @Schema(example = "CAD", description = "ISO 4217 currency code") String currency
  ) {
  }

  @Schema(description = "Request to update base currency")
  public record UpdateCurrencyRequest(
      @NotBlank @Size(min = 3, max = 3) @Pattern(regexp = "[A-Z]{3}") @Schema(example = "USD", description = "ISO 4217 currency code") String currency
  ) {
  }
}