package dev.cerbos.spring;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

@FunctionalInterface
public interface CerbosAuthorizationRequestFactory {

    CerbosEvaluationRequest create(Authentication authentication, RequestAuthorizationContext context);
}
