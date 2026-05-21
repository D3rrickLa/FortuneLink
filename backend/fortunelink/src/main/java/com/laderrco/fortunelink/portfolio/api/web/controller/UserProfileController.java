package com.laderrco.fortunelink.portfolio.api.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.laderrco.fortunelink.portfolio.application.services.UserProfileService;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/profile")
@Tag(name = "User Profile", description = "Endpoints for managing authenticated user profile information")
public class UserProfileController {
  private final UserProfileService profileService;

  @GetMapping
  @Operation(summary = "Get user profile", description = "Returns the authenticated user's profile")
  @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
  public ResponseEntity<UserProfileResponse> getProfile(@AuthenticatedUser UserId userId) {
    return ResponseEntity.ok(new UserProfileResponse(profileService.getFullName(userId)));
  }

  @PutMapping
  @Operation(summary = "Update full name", description = "Updates the authenticated user's display name")
  @ApiResponse(responseCode = "204", description = "Profile updated successfully")
  public ResponseEntity<Void> updateProfile(@AuthenticatedUser UserId userId,
      @Valid @RequestBody UpdateProfileRequest request) {
    profileService.updateFullName(userId, request.fullName());
    return ResponseEntity.noContent().build();
  }

  // DTOs

  @Schema(description = "Authenticated user profile")
  public record UserProfileResponse(@Schema(example = "Jane Doe", description = "User display name") String fullName) {
  }

  @Schema(description = "Request to update user profile")
  public record UpdateProfileRequest(
      @NotBlank @Size(min = 1, max = 100) @Schema(example = "Jane Doe", description = "Updated display name") String fullName) {
  }
}