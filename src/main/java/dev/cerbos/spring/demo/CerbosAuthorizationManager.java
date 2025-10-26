package dev.cerbos.spring.demo;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

public class CerbosAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CerbosAuthorizationManager.class);

    private final CerbosAuthorizationService authorizationService;

    public CerbosAuthorizationManager(CerbosAuthorizationService authorizationService) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get();
        HttpServletRequest request = context.getRequest();
        if (authentication == null || request == null) {
            return new AuthorizationDecision(false);
        }
        try {
            boolean allowed = authorizationService.isAllowed(authentication, request);
            return new AuthorizationDecision(allowed);
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LOGGER.error("Unexpected error while invoking Cerbos", ex);
            throw new AccessDeniedException("Unexpected error while invoking Cerbos", ex);
        }
    }
}
