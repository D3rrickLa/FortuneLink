package com.laderrco.fortunelink.portfolio.api.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.laderrco.fortunelink.portfolio.application.commands.UpdateUserPreferencesCommand;
import com.laderrco.fortunelink.portfolio.application.services.UserPreferencesService;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserPreferences;
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

  @Operation(summary = "Get user preferences", description = "Returns the authenticated user's preferences")
  @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully")
  @GetMapping
  public ResponseEntity<UserPreferencesResponse> getPreferences(@AuthenticatedUser UserId userId) {

    UserPreferences preferences = preferencesService.get(userId);

    return ResponseEntity.ok(new UserPreferencesResponse(preferences.getBaseCurrency().getCode(),
            preferences.isEmailNotifications(), preferences.isPriceAlerts(), preferences.getDateFormat()));
  }

  @Operation(summary = "Update user preferences", description = "Updates the authenticated user's preferences")
  @ApiResponse(responseCode = "204", description = "Preferences updated successfully")
  @PutMapping
  public ResponseEntity<Void> updatePreferences(
      @AuthenticatedUser UserId userId,
      @Valid @RequestBody UpdateUserPreferencesRequest request) {

    preferencesService.updatePreferences(userId, new UpdateUserPreferencesCommand(
        Currency.of(request.baseCurrency().toUpperCase()), request.emailNotifications(),
        request.priceAlerts(), request.dateFormat()));

    return ResponseEntity.noContent().build();
  }

  // DTOs

  @Schema(description = "User preferences response")
  public record UserPreferencesResponse(

      @Schema(example = "CAD", description = "ISO 4217 currency code") String baseCurrency,

      @Schema(example = "true", description = "Whether email notifications are enabled") boolean emailNotifications,

      @Schema(example = "true", description = "Whether price alerts are enabled") boolean priceAlerts,

      @Schema(example = "YYYY-MM-DD", description = "Preferred date format") String dateFormat) {
  }

  @Schema(description = "Request to update user preferences")
  public record UpdateUserPreferencesRequest(

      @NotBlank @Size(min = 3, max = 3) @Pattern(regexp = "[A-Z]{3}") @Schema(example = "USD", description = "ISO 4217 currency code") String baseCurrency,

      @Schema(example = "true", description = "Whether email notifications are enabled") boolean emailNotifications,

      @Schema(example = "true", description = "Whether price alerts are enabled") boolean priceAlerts,

      @NotBlank @Schema(example = "YYYY-MM-DD", description = "Preferred date format") String dateFormat) {
  }
}