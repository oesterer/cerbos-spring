package dev.cerbos.spring.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.cerbos.api.v1.request.Request;
import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CheckResourcesRequestBuilder;
import dev.cerbos.sdk.CheckResourcesResult;
import dev.cerbos.sdk.CheckResult;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.ResourceAction;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

class CerbosAuthorizationServiceTest {

    @Mock
    private CerbosBlockingClient cerbosBlockingClient;

    @Mock
    private CheckResourcesRequestBuilder requestBuilder;

    @Mock
    private CheckResourcesResult checkResourcesResult;

    @Mock
    private CheckResult checkResult;

    private CerbosAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        CerbosProperties properties = new CerbosProperties();
        authorizationService = new CerbosAuthorizationService(cerbosBlockingClient, properties);
    }

    @Test
    void shouldAllowWhenCerbosApproves() {
        when(cerbosBlockingClient.batch(any(Principal.class))).thenReturn(requestBuilder);
        when(requestBuilder.addResources(any(ResourceAction.class))).thenReturn(requestBuilder);
        when(requestBuilder.check()).thenReturn(checkResourcesResult);
        when(checkResourcesResult.find("/documents/123"))
                .thenReturn(Optional.of(checkResult));
        when(checkResult.isAllowed("read")).thenReturn(true);

        Authentication authentication = buildAuthentication();
        HttpServletRequest request = buildRequest("GET", "/documents/123");

        boolean allowed = authorizationService.isAllowed(authentication, request);

        assertThat(allowed).isTrue();

        ArgumentCaptor<Principal> principalCaptor = ArgumentCaptor.forClass(Principal.class);
        verify(cerbosBlockingClient).batch(principalCaptor.capture());
        assertThat(principalCaptor.getValue().toPrincipal().getRolesList()).containsExactly("employee");

        ArgumentCaptor<ResourceAction> resourceCaptor = ArgumentCaptor.forClass(ResourceAction.class);
        verify(requestBuilder).addResources(resourceCaptor.capture());
        Request.CheckResourcesRequest.ResourceEntry resourceEntry = resourceCaptor.getValue().toResourceEntry();
        assertThat(resourceEntry.getResource().getKind()).isEqualTo("http_request");
        assertThat(resourceEntry.getActionsList()).containsExactly("read");
    }

    @Test
    void shouldDenyWhenNoResultReturned() {
        when(cerbosBlockingClient.batch(any(Principal.class))).thenReturn(requestBuilder);
        when(requestBuilder.addResources(any(ResourceAction.class))).thenReturn(requestBuilder);
        when(requestBuilder.check()).thenReturn(checkResourcesResult);
        when(checkResourcesResult.find("/documents/123")).thenReturn(Optional.empty());

        Authentication authentication = buildAuthentication();
        HttpServletRequest request = buildRequest("GET", "/documents/123");

        boolean allowed = authorizationService.isAllowed(authentication, request);

        assertThat(allowed).isFalse();
    }

    @Test
    void shouldWrapRuntimeExceptions() {
        when(cerbosBlockingClient.batch(any(Principal.class))).thenReturn(requestBuilder);
        when(requestBuilder.addResources(any(ResourceAction.class))).thenReturn(requestBuilder);
        when(requestBuilder.check()).thenThrow(new RuntimeException("boom"));

        Authentication authentication = buildAuthentication();
        HttpServletRequest request = buildRequest("GET", "/documents/123");

        assertThatThrownBy(() -> authorizationService.isAllowed(authentication, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cerbos");
    }

    private Authentication buildAuthentication() {
        User user = new User("alice", "password", List.of(() -> "ROLE_employee"));
        return new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
    }

    private HttpServletRequest buildRequest(String method, String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getQueryString()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeaderNames()).thenReturn(java.util.Collections.emptyEnumeration());
        when(request.getHeaders(eq("any"))).thenReturn(java.util.Collections.emptyEnumeration());
        return request;
    }
}
