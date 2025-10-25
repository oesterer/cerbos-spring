package dev.cerbos.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

class CerbosEvaluationServiceTest {

    @Mock
    private CerbosBlockingClient cerbosBlockingClient;

    @Mock
    private CheckResourcesRequestBuilder requestBuilder;

    @Mock
    private CheckResourcesResult checkResourcesResult;

    @Mock
    private CheckResult checkResult;

    private CerbosEvaluationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        CerbosAuthorizationProperties properties = new CerbosAuthorizationProperties();
        service = new CerbosEvaluationService(cerbosBlockingClient, properties);
    }

    @Test
    void shouldAuthorizeWhenCerbosAllowsAction() {
        when(cerbosBlockingClient.batch(any(Principal.class))).thenReturn(requestBuilder);
        when(requestBuilder.addResources(any(ResourceAction.class))).thenReturn(requestBuilder);
        when(requestBuilder.check()).thenReturn(checkResourcesResult);
        when(checkResourcesResult.find("/documents/alpha"))
                .thenReturn(Optional.of(checkResult));
        when(checkResult.isAllowed("read")).thenReturn(true);

        CerbosEvaluationRequest request = new CerbosEvaluationRequest(
                "alice",
                List.of("ROLE_employee", "manager"),
                Map.of("department", "sales"),
                "document",
                "/documents/alpha",
                Map.of("owner", "alice"),
                "read",
                null,
                null,
                null);

        boolean authorized = service.isAuthorized(request);

        assertThat(authorized).isTrue();

        ArgumentCaptor<Principal> principalCaptor = ArgumentCaptor.forClass(Principal.class);
        verify(cerbosBlockingClient).batch(principalCaptor.capture());
        assertThat(principalCaptor.getValue().toPrincipal().getRolesList())
                .containsExactlyInAnyOrder("employee", "manager");

        ArgumentCaptor<ResourceAction> resourceCaptor = ArgumentCaptor.forClass(ResourceAction.class);
        verify(requestBuilder).addResources(resourceCaptor.capture());
        Request.CheckResourcesRequest.ResourceEntry resourceEntry = resourceCaptor.getValue().toResourceEntry();
        assertThat(resourceEntry.getActionsList()).containsExactly("read");
        assertThat(resourceEntry.getResource().getKind()).isEqualTo("document");
        assertThat(resourceEntry.getResource().getAttrMap().get("owner").getStringValue()).isEqualTo("alice");
    }

    @Test
    void shouldReturnFalseWhenActionDenied() {
        when(cerbosBlockingClient.batch(any(Principal.class))).thenReturn(requestBuilder);
        when(requestBuilder.addResources(any(ResourceAction.class))).thenReturn(requestBuilder);
        when(requestBuilder.check()).thenReturn(checkResourcesResult);
        when(checkResourcesResult.find("/documents/alpha")).thenReturn(Optional.empty());

        CerbosEvaluationRequest request = new CerbosEvaluationRequest(
                "bob",
                List.of("auditor"),
                Map.of(),
                "document",
                "/documents/alpha",
                Map.of(),
                "read",
                null,
                null,
                null);

        boolean authorized = service.isAuthorized(request);

        assertThat(authorized).isFalse();
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenCerbosFails() {
        when(cerbosBlockingClient.batch(any(Principal.class))).thenReturn(requestBuilder);
        when(requestBuilder.addResources(any(ResourceAction.class))).thenReturn(requestBuilder);
        when(requestBuilder.check())
                .thenThrow(new RuntimeException("down"));

        CerbosEvaluationRequest request = new CerbosEvaluationRequest(
                "carol",
                List.of("admin"),
                Map.of(),
                "document",
                "/documents/beta",
                Map.of(),
                "delete",
                null,
                null,
                null);

        assertThatThrownBy(() -> service.isAuthorized(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Failed to evaluate authorization");
    }

    @Test
    void shouldPropagatePolicyVersionOverride() {
        when(cerbosBlockingClient.batch(any(Principal.class))).thenReturn(requestBuilder);
        when(requestBuilder.addResources(any(ResourceAction.class))).thenReturn(requestBuilder);
        when(requestBuilder.check()).thenReturn(checkResourcesResult);
        when(checkResourcesResult.find("/documents/gamma"))
                .thenReturn(Optional.of(checkResult));
        when(checkResult.isAllowed("update")).thenReturn(true);

        CerbosEvaluationRequest request = new CerbosEvaluationRequest(
                "dave",
                List.of("editor"),
                Map.of(),
                "document",
                "/documents/gamma",
                Map.of(),
                "update",
                "v2024",
                "teamA",
                "scopeA");

        service.isAuthorized(request);

        ArgumentCaptor<ResourceAction> resourceCaptor = ArgumentCaptor.forClass(ResourceAction.class);
        verify(requestBuilder).addResources(resourceCaptor.capture());
        Request.CheckResourcesRequest.ResourceEntry resourceEntry = resourceCaptor.getValue().toResourceEntry();
        assertThat(resourceEntry.getResource().getPolicyVersion()).isEqualTo("v2024");
        assertThat(resourceEntry.getResource().getScope()).isEqualTo("scopeA");
    }
}
