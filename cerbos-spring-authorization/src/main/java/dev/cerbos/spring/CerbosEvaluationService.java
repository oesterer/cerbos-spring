package dev.cerbos.spring;

import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CerbosException;
import dev.cerbos.sdk.CheckResourcesResult;
import dev.cerbos.sdk.builders.AttributeValue;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.ResourceAction;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;

public class CerbosEvaluationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CerbosEvaluationService.class);

    private final CerbosBlockingClient client;

    private final CerbosAuthorizationProperties properties;

    public CerbosEvaluationService(CerbosBlockingClient client, CerbosAuthorizationProperties properties) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public boolean isAuthorized(CerbosEvaluationRequest request) {
        CheckResourcesResult result = evaluate(request);
        return result.find(request.resourceId())
                .map(checkResult -> checkResult.isAllowed(request.action()))
                .orElse(false);
    }

    public CheckResourcesResult evaluate(CerbosEvaluationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (!StringUtils.hasText(request.principalId())) {
            throw new IllegalArgumentException("principalId must not be null or blank");
        }
        if (!StringUtils.hasText(request.action())) {
            throw new IllegalArgumentException("action must not be null or blank");
        }
        if (!StringUtils.hasText(request.resourceId())) {
            throw new IllegalArgumentException("resourceId must not be null or blank");
        }

        String[] roleArray = sanitizeRoles(request.principalRoles());
        Principal principal = Principal.newInstance(request.principalId(), roleArray);
        String policyVersion = resolvePolicyVersion(request);
        if (StringUtils.hasText(policyVersion)) {
            principal.withPolicyVersion(policyVersion);
        }
        String principalScope = resolvePrincipalScope(request);
        if (StringUtils.hasText(principalScope)) {
            principal.withScope(principalScope);
        }
        Map<String, AttributeValue> principalAttrs =
                CerbosAttributeValueConverter.fromObjectMap(request.principalAttributes());
        principal.withAttributes(principalAttrs);

        String resourceKind = resolveResourceKind(request);
        if (!StringUtils.hasText(resourceKind)) {
            throw new IllegalArgumentException("resourceKind must not be null or blank");
        }
        ResourceAction resource = ResourceAction.newInstance(resourceKind, request.resourceId());
        if (StringUtils.hasText(policyVersion)) {
            resource.withPolicyVersion(policyVersion);
        }
        String resourceScope = resolveResourceScope(request);
        if (StringUtils.hasText(resourceScope)) {
            resource.withScope(resourceScope);
        }
        Map<String, AttributeValue> resourceAttrs =
                CerbosAttributeValueConverter.fromObjectMap(request.resourceAttributes());
        resource.withAttributes(resourceAttrs);
        resource.withActions(request.action());

        try {
            return client.batch(principal).addResources(resource).check();
        } catch (RuntimeException ex) {
            if (ex instanceof CerbosException) {
                LOGGER.warn(
                        "Cerbos authorization failed for principal {} on resource {}:{} action {}",
                        request.principalId(),
                        resourceKind,
                        request.resourceId(),
                        request.action(),
                        ex);
            } else {
                LOGGER.error(
                        "Unexpected error while invoking Cerbos for principal {} on resource {}:{} action {}",
                        request.principalId(),
                        resourceKind,
                        request.resourceId(),
                        request.action(),
                        ex);
            }
            throw new AccessDeniedException("Failed to evaluate authorization using Cerbos", ex);
        }
    }

    private String resolvePolicyVersion(CerbosEvaluationRequest request) {
        if (StringUtils.hasText(request.policyVersion())) {
            return request.policyVersion();
        }
        return properties.getPolicyVersion();
    }

    private String resolvePrincipalScope(CerbosEvaluationRequest request) {
        if (StringUtils.hasText(request.principalScope())) {
            return request.principalScope();
        }
        return properties.getPrincipalScope();
    }

    private String resolveResourceScope(CerbosEvaluationRequest request) {
        if (StringUtils.hasText(request.resourceScope())) {
            return request.resourceScope();
        }
        return properties.getResourceScope();
    }

    private String resolveResourceKind(CerbosEvaluationRequest request) {
        if (StringUtils.hasText(request.resourceKind())) {
            return request.resourceKind();
        }
        return properties.getResourceKind();
    }

    private String[] sanitizeRoles(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return new String[0];
        }
        Set<String> filtered = roles.stream()
                .filter(StringUtils::hasText)
                .map(role -> role.replaceFirst("^ROLE_", ""))
                .collect(Collectors.toSet());
        return filtered.toArray(String[]::new);
    }
}
