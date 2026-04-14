package com.laderrco.fortunelink.portfolio.infrastructure.config.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserResolverTest {

  @Mock
  private AuthenticationUserService authenticationUserService;

  private AuthenticatedUserResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new AuthenticatedUserResolver(authenticationUserService);
  }

  void testMethod(@AuthenticatedUser UserId annotatedUserId, @AuthenticatedUser UUID annotatedUuid,
      UserId unannotatedUserId, @AuthenticatedUser String wrongTypeString) {
  }

  @Test
  void shouldSupportUserIdWithAnnotation() throws NoSuchMethodException {
    MethodParameter param = getParam(0);
    assertThat(resolver.supportsParameter(param)).isTrue();
  }

  @Test
  void shouldSupportUUIDWithAnnotation() throws NoSuchMethodException {
    MethodParameter param = getParam(1);
    assertThat(resolver.supportsParameter(param)).isTrue();
  }

  @Test
  void shouldNotSupportParametersWithoutAnnotation() throws NoSuchMethodException {
    MethodParameter param = getParam(2);
    assertThat(resolver.supportsParameter(param)).isFalse();
  }

  @Test
  void shouldNotSupportWrongTypes() throws NoSuchMethodException {
    MethodParameter param = getParam(3);
    assertThat(resolver.supportsParameter(param)).isFalse();
  }

  @Test
  void shouldResolveUserId() throws NoSuchMethodException {
    UUID expectedUuid = UUID.randomUUID();
    when(authenticationUserService.getCurrentUser()).thenReturn(expectedUuid);

    MethodParameter param = getParam(0);
    Object result = resolver.resolveArgument(param, null, null, null);

    assertThat(result).isInstanceOf(UserId.class);

    assertThat(result.toString()).contains(expectedUuid.toString());
  }

  private MethodParameter getParam(int index) throws NoSuchMethodException {

    var method = AuthenticatedUserResolverTest.class.getDeclaredMethod("testMethod", UserId.class,
        UUID.class, UserId.class, String.class);
    return new MethodParameter(method, index);
  }
}