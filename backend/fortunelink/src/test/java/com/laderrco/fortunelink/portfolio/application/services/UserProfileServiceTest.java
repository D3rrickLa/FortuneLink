package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserProfile;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserProfileRepository;
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
@DisplayName("UserProfileService Unit Tests")
class UserProfileServiceTest {

  private final UserId userId = new UserId(UUID.randomUUID());
  @Mock
  private UserProfileRepository repository;
  @InjectMocks
  private UserProfileService service;

  @Nested
  @DisplayName("Get Full Name")
  class GetFullName {

    @Test
    @DisplayName("Should return the user's full name when profile exists")
    void shouldReturnFullNameWhenProfileExists() {
      // Arrange
      UserProfile profile = new UserProfile(userId, "John Doe");
      when(repository.findById(userId)).thenReturn(Optional.of(profile));

      // Act
      String fullName = service.getFullName(userId);

      // Assert
      assertThat(fullName).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should return an empty string when profile does not exist")
    void shouldReturnEmptyStringWhenProfileMissing() {
      // Arrange
      when(repository.findById(userId)).thenReturn(Optional.empty());

      // Act
      String fullName = service.getFullName(userId);

      // Assert
      assertThat(fullName).isEmpty();
    }
  }

  @Nested
  @DisplayName("Update Full Name")
  class UpdateFullName {

    @Test
    @DisplayName("Should update and save existing profile when it already exists")
    void shouldUpdateExistingProfile() {
      // Arrange
      UserProfile existingProfile = new UserProfile(userId, "Jane Doe");
      when(repository.findById(userId)).thenReturn(Optional.of(existingProfile));

      // Act
      service.updateFullName(userId, "Jane Smith");

      // Assert
      // Verifies the state change on the existing entity instance
      assertThat(existingProfile.getFullName()).isEqualTo("Jane Smith");
      verify(repository).save(existingProfile);
    }

    @Test
    @DisplayName("Should instantiate, update, and save a brand new profile if none exists")
    void shouldCreateAndSaveNewProfileWhenMissing() {
      // Arrange
      when(repository.findById(userId)).thenReturn(Optional.empty());

      // Act
      service.updateFullName(userId, "New User");

      // Assert
      // Verifies that a UserProfile object with the correct details was passed into save()
      verify(repository).save(any(UserProfile.class));

      // Optional/Alternative: capture the argument if you need to assert internal details of the new object
      verify(repository).save(org.mockito.Mockito.argThat(
          profile -> profile.getUserId().equals(userId) && "New User".equals(
              profile.getFullName())));
    }
  }
}