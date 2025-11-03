package dev.cerbos.spring.demo;

import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CheckResourcesResult;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.ResourceAction;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

public class CerbosAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CerbosAuthorizationService.class);

    private final CerbosBlockingClient client;
    private final CerbosProperties properties;

    public CerbosAuthorizationService(CerbosBlockingClient client, CerbosProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public boolean isAllowed(Authentication authentication, HttpServletRequest request) {
        CerbosProperties.Http httpConfig = properties.getHttp();
        String action = httpConfig.resolveAction(request.getMethod());
        String resourceKind = httpConfig.getResourceKind();
        Map<String, Object> resourceAttributes = buildResourceAttributes(request);
        return checkPermission(authentication, resourceKind, request.getRequestURI(), action, resourceAttributes, Map.of());
    }

    public boolean checkPermission(
            Authentication authentication,
            String resourceKind,
            String resourceId,
            String action,
            Map<String, Object> resourceAttributes,
            Map<String, Object> principalAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        List<String> roles = extractRoles(authentication.getAuthorities());
        Principal principal = Principal.newInstance(authentication.getName(), roles.toArray(String[]::new));
        Map<String, Object> mergedPrincipalAttrs = buildPrincipalAttributes(authentication, roles, principalAttributes);
        principal.withAttributes(AttributeValueConverter.fromObjectMap(mergedPrincipalAttrs));

        ResourceAction resource = ResourceAction.newInstance(resourceKind, resourceId);
        Map<String, Object> safeResourceAttributes = resourceAttributes != null ? resourceAttributes : Map.of();
        resource.withAttributes(AttributeValueConverter.fromObjectMap(safeResourceAttributes));
        resource.withActions(action);

        try {
            CheckResourcesResult result = client.batch(principal).addResources(resource).check();
            return result.find(resourceId)
                    .map(checkResult -> checkResult.isAllowed(action))
                    .orElse(false);
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to evaluate authorization via Cerbos", ex);
            throw new AccessDeniedException("Failed to evaluate authorization via Cerbos", ex);
        }
    }

    private Map<String, Object> buildPrincipalAttributes(
            Authentication authentication, List<String> roles, Map<String, Object> additionalAttributes) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("roles", roles);
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            attributes.put("accountNonExpired", userDetails.isAccountNonExpired());
            attributes.put("accountNonLocked", userDetails.isAccountNonLocked());
            attributes.put("credentialsNonExpired", userDetails.isCredentialsNonExpired());
            attributes.put("enabled", userDetails.isEnabled());
        }
        if (additionalAttributes != null && !additionalAttributes.isEmpty()) {
            attributes.putAll(additionalAttributes);
        }
        return attributes;
    }

    private Map<String, Object> buildResourceAttributes(HttpServletRequest request) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("method", request.getMethod());
        attributes.put("path", request.getRequestURI());
        attributes.put("query", request.getQueryString());
        attributes.put("remoteAddr", request.getRemoteAddr());
        attributes.put("headers", extractHeaders(request));
        attributes.put("segments", extractSegments(request.getRequestURI()));
        return attributes;
    }

    private Map<String, List<String>> extractHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name ->
                headers.put(name.toLowerCase(), Collections.list(request.getHeaders(name))));
        return headers;
    }

    private List<String> extractSegments(String uri) {
        if (!StringUtils.hasText(uri)) {
            return List.of();
        }
        String sanitized = uri.startsWith("/") ? uri.substring(1) : uri;
        if (!StringUtils.hasText(sanitized)) {
            return List.of();
        }
        return List.of(sanitized.split("/"));
    }

    private List<String> extractRoles(Iterable<? extends GrantedAuthority> authorities) {
        if (authorities == null) {
            return List.of();
        }
        return (
                        authorities instanceof Collection<? extends GrantedAuthority> collection
                                ? collection.stream()
                                : java.util.stream.StreamSupport.stream(authorities.spliterator(), false))
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .map(role -> role.replaceFirst("^ROLE_", ""))
                .collect(Collectors.toList());
    }
}
