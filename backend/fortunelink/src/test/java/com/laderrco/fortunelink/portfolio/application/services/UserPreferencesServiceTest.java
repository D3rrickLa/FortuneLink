package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.commands.UpdateUserPreferencesCommand;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserPreferences;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserPreferencesRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPreferencesService Unit Tests")
class UserPreferencesServiceTest {

  @Mock
  private UserPreferencesRepository repository;

  @InjectMocks
  private UserPreferencesService service;

  private final UserId userId = new UserId(UUID.randomUUID());

  @Nested
  @DisplayName("Get User Preferences")
  class GetPreferences {

    @Test
    @DisplayName("Should return preferences when they exist in repository")
    void shouldReturnPreferencesWhenTheyExist() {
      // Arrange
      UserPreferences existingPrefs = new UserPreferences(userId, Currency.CAD, true, true, "MM/DD/YYYY");
      when(repository.findById(userId)).thenReturn(Optional.of(existingPrefs));

      // Act
      UserPreferences result = service.get(userId);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.getBaseCurrency()).isEqualTo(Currency.CAD);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when preferences are not initialized")
    void shouldThrowExceptionWhenPreferencesMissing() {
      // Arrange
      when(repository.findById(userId)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.get(userId))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Preferences not initialized");
    }
  }

  @Nested
  @DisplayName("Update User Preferences")
  class UpdatePreferences {

    @Test
    @DisplayName("Should update all preference fields and save when preferences exist")
    void shouldUpdatePreferencesSuccessfully() {
      // Arrange
      UserPreferences existingPrefs = new UserPreferences(userId, Currency.CAD, false, false, "MM/DD/YYYY");
      when(repository.findById(userId)).thenReturn(Optional.of(existingPrefs));

      UpdateUserPreferencesCommand command = new UpdateUserPreferencesCommand(
          Currency.USD,
          true,
          true,
          "YYYY-MM-DD"
      );

      // Act
      service.updatePreferences(userId, command);

      // Assert
      assertThat(existingPrefs.getBaseCurrency()).isEqualTo(Currency.USD);
      assertThat(existingPrefs.isEmailNotifications()).isTrue();
      assertThat(existingPrefs.isPriceAlerts()).isTrue();
      assertThat(existingPrefs.getDateFormat()).isEqualTo("YYYY-MM-DD");

      verify(repository).save(existingPrefs);
    }

    @Test
    @DisplayName("Should bubble up exception and abort update if preferences do not exist")
    void shouldNotUpdateIfPreferencesMissing() {
      // Arrange
      when(repository.findById(userId)).thenReturn(Optional.empty());
      UpdateUserPreferencesCommand command = new UpdateUserPreferencesCommand(Currency.USD, true, true, "YYYY-MM-DD");

      // Act & Assert
      assertThatThrownBy(() -> service.updatePreferences(userId, command))
          .isInstanceOf(IllegalStateException.class);

      verify(repository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Create Default Preferences")
  class CreateDefaultPreferences {

    @Test
    @DisplayName("Should create, save, and return new default settings if none exist")
    void shouldCreateNewDefaultIfNoneExist() {
      // Arrange
      when(repository.existsById(userId)).thenReturn(false);

      // Capturing the save behavior
      when(repository.save(any(UserPreferences.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // Act
      UserPreferences result = service.createDefault(userId);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.getBaseCurrency()).isEqualTo(Currency.CAD);
      assertThat(result.isEmailNotifications()).isTrue();
      assertThat(result.isPriceAlerts()).isTrue();
      assertThat(result.getDateFormat()).isEqualTo("MM/DD/YYYY");

      verify(repository).save(any(UserPreferences.class));
    }

    @Test
    @DisplayName("Should return existing settings without creating a new default copy if they already exist")
    void shouldReturnExistingIfAlreadyPresent() {
      // Arrange
      UserPreferences existingPrefs = new UserPreferences(userId, Currency.USD, false, false, "DD-MM-YYYY");
      when(repository.existsById(userId)).thenReturn(true);
      when(repository.findById(userId)).thenReturn(Optional.of(existingPrefs));

      // Act
      UserPreferences result = service.createDefault(userId);

      // Assert
      assertThat(result).isSameAs(existingPrefs);
      verify(repository, never()).save(any(UserPreferences.class));
    }
  }
}