package dev.cerbos.spring.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

class CerbosAuthorizationManagerTest {

    private CerbosAuthorizationService authorizationService;
    private CerbosAuthorizationManager authorizationManager;

    @BeforeEach
    void setUp() {
        authorizationService = mock(CerbosAuthorizationService.class);
        authorizationManager = new CerbosAuthorizationManager(authorizationService);
    }

    @Test
    void shouldDenyWhenAuthenticationMissing() {
        RequestAuthorizationContext context = mock(RequestAuthorizationContext.class);
        when(context.getRequest()).thenReturn(mock(HttpServletRequest.class));

        AuthorizationDecision decision = authorizationManager.check(() -> null, context);

        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    void shouldDelegateToService() {
        Authentication authentication = mock(Authentication.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        RequestAuthorizationContext context = mock(RequestAuthorizationContext.class);
        when(context.getRequest()).thenReturn(request);
        when(authorizationService.isAllowed(authentication, request)).thenReturn(true);

        AuthorizationDecision decision = authorizationManager.check(() -> authentication, context);

        assertThat(decision.isGranted()).isTrue();
    }

    @Test
    void shouldWrapUnexpectedRuntimeException() {
        Authentication authentication = mock(Authentication.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        RequestAuthorizationContext context = mock(RequestAuthorizationContext.class);
        when(context.getRequest()).thenReturn(request);
        when(authorizationService.isAllowed(authentication, request))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> authorizationManager.check(() -> authentication, context))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unexpected error");
    }
}
