package com.laderrco.fortunelink.portfolio.api.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.laderrco.fortunelink.portfolio.application.services.UserPreferenceService;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@Tag(name = "User Preferences", description = "Endpoints for managing user-level preferences such as base currency")
public class UserPreferenceController {
  private final UserPreferenceService preferenceService;

  // GET /api/v1/users/me/preferences/currency 

  @Operation(summary = "Get base currency", description = "Returns the authenticated user's preferred reporting currency")
  @ApiResponse(responseCode = "200", description = "Currency retrieved successfully")
  @GetMapping("/currency")
  public ResponseEntity<CurrencyPreferenceResponse> getBaseCurrency(
      @AuthenticatedUser UserId userId) {
    Currency currency = preferenceService.getBaseCurrency(userId);
    return ResponseEntity.ok(new CurrencyPreferenceResponse(currency.getCode()));
  }

  // PUT /api/v1/users/me/preferences/currency 

  @Operation(summary = "Update base currency", description = "Updates the authenticated user's preferred reporting currency")
  @ApiResponse(responseCode = "204", description = "Currency updated successfully")
  @PutMapping("/currency")
  public ResponseEntity<Void> updateBaseCurrency(
      @AuthenticatedUser UserId userId,
      @Valid @RequestBody UpdateCurrencyRequest request) {
    Currency currency = Currency.of(request.currency().toUpperCase());
    preferenceService.updateBaseCurrency(userId, currency);
    return ResponseEntity.noContent().build();
  }

  // DTOs -------------------------------

  @Schema(description = "Base currency preference response")
  public record CurrencyPreferenceResponse(
      @Schema(example = "CAD", description = "ISO 4217 currency code") String currency) {
  }

  @Schema(description = "Request to update base currency preference")
  public record UpdateCurrencyRequest(

      @Schema(example = "USD", description = "ISO 4217 currency code (3 uppercase letters)") @NotNull @Size(min = 3, max = 3) @Pattern(regexp = "[A-Z]{3}") String currency

  ) {
  }
}