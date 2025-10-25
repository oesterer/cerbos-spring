package dev.cerbos.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

class CerbosAuthorizationManagerTest {

    private CerbosEvaluationService evaluationService;
    private CerbosAuthorizationRequestFactory requestFactory;
    private CerbosAuthorizationManager manager;

    @BeforeEach
    void setUp() {
        evaluationService = mock(CerbosEvaluationService.class);
        requestFactory = mock(CerbosAuthorizationRequestFactory.class);
        manager = new CerbosAuthorizationManager(evaluationService, requestFactory);
    }

    @Test
    void shouldDenyWhenAuthenticationMissing() {
        AuthorizationDecision decision = manager.check(() -> null, mock(RequestAuthorizationContext.class));
        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    void shouldDenyWhenAuthenticationNotAuthenticated() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);
        AuthorizationDecision decision = manager.check(() -> authentication, mock(RequestAuthorizationContext.class));
        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    void shouldDelegateToEvaluationService() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice");
        RequestAuthorizationContext context = mock(RequestAuthorizationContext.class);

        CerbosEvaluationRequest request = new CerbosEvaluationRequest(
                "alice",
                java.util.List.of("admin"),
                java.util.Map.of(),
                "document",
                "resource-1",
                java.util.Map.of(),
                "delete",
                null,
                null,
                null);
        when(requestFactory.create(authentication, context)).thenReturn(request);
        when(evaluationService.isAuthorized(request)).thenReturn(true);

        AuthorizationDecision decision = manager.check(() -> authentication, context);

        assertThat(decision.isGranted()).isTrue();
        verify(evaluationService).isAuthorized(request);
    }

    @Test
    void shouldRethrowAccessDeniedException() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("bob");
        RequestAuthorizationContext context = mock(RequestAuthorizationContext.class);

        CerbosEvaluationRequest request = new CerbosEvaluationRequest(
                "bob",
                java.util.List.of(),
                java.util.Map.of(),
                "document",
                "resource-2",
                java.util.Map.of(),
                "read",
                null,
                null,
                null);
        when(requestFactory.create(authentication, context)).thenReturn(request);
        when(evaluationService.isAuthorized(request))
                .thenThrow(new AccessDeniedException("service failure"));

        assertThatThrownBy(() -> manager.check(() -> authentication, context))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("service failure");
    }

    @Test
    void shouldWrapUnexpectedRuntimeException() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("carol");
        RequestAuthorizationContext context = mock(RequestAuthorizationContext.class);

        CerbosEvaluationRequest request = new CerbosEvaluationRequest(
                "carol",
                java.util.List.of(),
                java.util.Map.of(),
                "document",
                "resource-3",
                java.util.Map.of(),
                "update",
                null,
                null,
                null);
        when(requestFactory.create(authentication, context)).thenReturn(request);
        when(evaluationService.isAuthorized(request)).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> manager.check(() -> authentication, context))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cerbos authorization failed");
    }
}
