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
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String principalId = authentication.getName();
        List<String> roles = extractRoles(authentication.getAuthorities());
        Principal principal = Principal.newInstance(principalId, roles.toArray(String[]::new));
        Map<String, Object> principalAttrs = buildPrincipalAttributes(authentication, roles);
        principal.withAttributes(AttributeValueConverter.fromObjectMap(principalAttrs));

        CerbosProperties.Http httpConfig = properties.getHttp();
        String action = httpConfig.resolveAction(request.getMethod());
        String resourceKind = httpConfig.getResourceKind();

        ResourceAction resource = ResourceAction.newInstance(resourceKind, request.getRequestURI());
        resource.withAttributes(AttributeValueConverter.fromObjectMap(buildResourceAttributes(request)));
        resource.withActions(action);

        try {
            CheckResourcesResult result = client.batch(principal).addResources(resource).check();
            return result.find(request.getRequestURI())
                    .map(checkResult -> checkResult.isAllowed(action))
                    .orElse(false);
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to evaluate authorization via Cerbos", ex);
            throw new AccessDeniedException("Failed to evaluate authorization via Cerbos", ex);
        }
    }

    private Map<String, Object> buildPrincipalAttributes(Authentication authentication, List<String> roles) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("roles", roles);
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            attributes.put("accountNonExpired", userDetails.isAccountNonExpired());
            attributes.put("accountNonLocked", userDetails.isAccountNonLocked());
            attributes.put("credentialsNonExpired", userDetails.isCredentialsNonExpired());
            attributes.put("enabled", userDetails.isEnabled());
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
