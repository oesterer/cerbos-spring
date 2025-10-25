package dev.cerbos.spring;

import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

public class CerbosAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CerbosAuthorizationManager.class);

    private final CerbosEvaluationService evaluationService;
    private final CerbosAuthorizationRequestFactory requestFactory;

    public CerbosAuthorizationManager(
            CerbosEvaluationService evaluationService, CerbosAuthorizationRequestFactory requestFactory) {
        this.evaluationService = Objects.requireNonNull(evaluationService, "evaluationService must not be null");
        this.requestFactory = Objects.requireNonNull(requestFactory, "requestFactory must not be null");
    }

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null) {
            LOGGER.debug("No authentication found; denying access");
            return new AuthorizationDecision(false);
        }
        if (!auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            LOGGER.debug("Unauthenticated or anonymous principal {}; denying access", auth.getName());
            return new AuthorizationDecision(false);
        }

        CerbosEvaluationRequest request = requestFactory.create(auth, context);
        try {
            boolean allowed = evaluationService.isAuthorized(request);
            return new AuthorizationDecision(allowed);
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "Unexpected error while evaluating Cerbos authorization for principal {}", auth.getName(), ex);
            throw new AccessDeniedException("Cerbos authorization failed", ex);
        }
    }
}
