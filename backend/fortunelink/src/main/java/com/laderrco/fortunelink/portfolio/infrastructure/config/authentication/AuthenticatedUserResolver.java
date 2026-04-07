package com.laderrco.fortunelink.portfolio.infrastructure.config.authentication;

import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver implements HandlerMethodArgumentResolver {
  private final AuthenticationUserService authenticationUserService;

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(AuthenticatedUser.class) && (
        parameter.getParameterType().equals(UserId.class) || parameter.getParameterType()
            .equals(UUID.class));
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

    UUID rawId = authenticationUserService.getCurrentUser();

    if (parameter.getParameterType().equals(UserId.class)) {
      return UserId.fromString(rawId.toString());
    }

    return rawId;
  }
}