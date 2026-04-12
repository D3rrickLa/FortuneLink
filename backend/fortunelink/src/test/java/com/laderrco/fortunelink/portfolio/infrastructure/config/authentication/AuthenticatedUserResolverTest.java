package com.laderrco.fortunelink.portfolio.infrastructure.config.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;

import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserResolverTest {

  @Mock
  private AuthenticationUserService authenticationUserService;

  @Mock
  private MethodParameter methodParameter;

  private AuthenticatedUserResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new AuthenticatedUserResolver(authenticationUserService);
  }

  @Test
  void shouldSupportUserIdWithAnnotation() throws NoSuchMethodException {
    MethodParameter param = getParam(0); // Points to @AuthenticatedUser UserId
    assertThat(resolver.supportsParameter(param)).isTrue();
  }

  @Test
  void shouldSupportUUIDWithAnnotation() throws NoSuchMethodException {
    MethodParameter param = getParam(1); // Points to @AuthenticatedUser UUID
    assertThat(resolver.supportsParameter(param)).isTrue();
  }

  @Test
  void shouldNotSupportParametersWithoutAnnotation() throws NoSuchMethodException {
    MethodParameter param = getParam(2); // Points to UserId (no annotation)
    assertThat(resolver.supportsParameter(param)).isFalse();
  }

  @Test
  void shouldNotSupportWrongTypes() throws NoSuchMethodException {
    MethodParameter param = getParam(3); // Points to @AuthenticatedUser String
    assertThat(resolver.supportsParameter(param)).isFalse();
  }

  @Test
  void shouldResolveUserId() throws NoSuchMethodException {
    UUID expectedUuid = UUID.randomUUID();
    when(authenticationUserService.getCurrentUser()).thenReturn(expectedUuid);

    MethodParameter param = getParam(0);
    Object result = resolver.resolveArgument(param, null, null, null);

    assertThat(result).isInstanceOf(UserId.class);
    assertThat(result.toString()).isEqualTo(expectedUuid.toString());
  }

  // Helper to get MethodParameter for the dummy method
  private MethodParameter getParam(int index) throws NoSuchMethodException {
    var method = AuthenticatedUserResolverTest.class.getDeclaredMethod(
        "testMethod", UserId.class, UUID.class, UserId.class, String.class);
    return new MethodParameter(method, index);
  }
}