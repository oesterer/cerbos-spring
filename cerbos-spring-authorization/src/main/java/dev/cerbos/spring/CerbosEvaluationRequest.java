package dev.cerbos.spring;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public record CerbosEvaluationRequest(
        String principalId,
        Collection<String> principalRoles,
        Map<String, Object> principalAttributes,
        String resourceKind,
        String resourceId,
        Map<String, Object> resourceAttributes,
        String action,
        String policyVersion,
        String principalScope,
        String resourceScope) {

    public CerbosEvaluationRequest {
        principalRoles = principalRoles == null ? Collections.emptyList() : principalRoles;
        principalAttributes = principalAttributes == null ? Collections.emptyMap() : principalAttributes;
        resourceAttributes = resourceAttributes == null ? Collections.emptyMap() : resourceAttributes;
    }
}
