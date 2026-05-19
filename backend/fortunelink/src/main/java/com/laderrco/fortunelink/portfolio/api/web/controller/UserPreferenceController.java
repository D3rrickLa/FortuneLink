package com.laderrco.fortunelink.portfolio.api.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.laderrco.fortunelink.portfolio.application.exceptions.UserNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserPreferencesRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.UserJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories.JpaUserRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users/me/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {
    private final UserPreferencesRepository userPreferencesRepository;
    private final JpaUserRepository jpaUserRepository;

    // ── GET currency ─────────────────────────────────────────────

    @GetMapping("/currency")
    public ResponseEntity<CurrencyPreferenceResponse> getBaseCurrency(
            @AuthenticatedUser UserId userId
    ) {
        Currency currency = userPreferencesRepository.getBaseCurrency(userId);

        return ResponseEntity.ok(
                new CurrencyPreferenceResponse(currency.getCode())
        );
    }

    // ── PUT currency ─────────────────────────────────────────────

    @PutMapping("/currency")
    @Transactional
    public ResponseEntity<Void> updateBaseCurrency(
            @AuthenticatedUser UserId userId,
            @Valid @RequestBody UpdateCurrencyRequest request
    ) {
        Currency currency = Currency.of(request.currency());

        UserJpaEntity user = jpaUserRepository.findById(userId.id())
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.setBaseCurrency(currency.getCode());

        return ResponseEntity.noContent().build();
    }

    // ── DTOs ─────────────────────────────────────────────────────

    public record CurrencyPreferenceResponse(String currency) {}

    public record UpdateCurrencyRequest(
            @NotNull
            @Size(min = 3, max = 3)
            @Pattern(regexp = "[A-Z]{3}")
            String currency
    ) {}
}