package dev.cerbos.spring;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.StringUtils;

public class DefaultHttpCerbosRequestFactory implements CerbosAuthorizationRequestFactory {

    private final CerbosAuthorizationProperties properties;

    public DefaultHttpCerbosRequestFactory(CerbosAuthorizationProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public CerbosEvaluationRequest create(Authentication authentication, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        if (request == null) {
            throw new IllegalStateException("No HttpServletRequest available in authorization context");
        }

        List<String> roles = extractRoles(authentication.getAuthorities());

        Map<String, Object> principalAttributes = new LinkedHashMap<>();
        principalAttributes.put("authorities", roles);
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            principalAttributes.put("accountNonExpired", userDetails.isAccountNonExpired());
            principalAttributes.put("accountNonLocked", userDetails.isAccountNonLocked());
            principalAttributes.put("credentialsNonExpired", userDetails.isCredentialsNonExpired());
            principalAttributes.put("enabled", userDetails.isEnabled());
        }
        if (authentication.getDetails() instanceof Map<?, ?> details) {
            Map<String, Object> sanitizedDetails = new LinkedHashMap<>();
            details.forEach((key, value) -> {
                if (key != null) {
                    sanitizedDetails.put(String.valueOf(key), value);
                }
            });
            if (!sanitizedDetails.isEmpty()) {
                principalAttributes.put("details", sanitizedDetails);
            }
        }

        Map<String, Object> resourceAttributes = new LinkedHashMap<>();
        resourceAttributes.put("method", request.getMethod());
        resourceAttributes.put("path", request.getRequestURI());
        if (StringUtils.hasText(request.getQueryString())) {
            resourceAttributes.put("query", request.getQueryString());
        }
        resourceAttributes.put("remoteAddress", request.getRemoteAddr());
        resourceAttributes.put("contentType", request.getContentType());
        List<String> pathSegments = extractPathSegments(request.getRequestURI());
        resourceAttributes.put("segments", pathSegments);
        resourceAttributes.put("headers", extractHeaders(request));

        String action = properties.resolveAction(request.getMethod());
        return new CerbosEvaluationRequest(
                authentication.getName(),
                roles,
                principalAttributes,
                properties.getResourceKind(),
                request.getRequestURI(),
                resourceAttributes,
                action,
                null,
                null,
                null);
    }

    private List<String> extractRoles(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) {
            return Collections.emptyList();
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<String> extractPathSegments(String requestUri) {
        if (!StringUtils.hasText(requestUri)) {
            return Collections.emptyList();
        }
        String sanitized = requestUri.startsWith("/") ? requestUri.substring(1) : requestUri;
        if (!StringUtils.hasText(sanitized)) {
            return Collections.emptyList();
        }
        String[] parts = sanitized.split("/");
        List<String> segments = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                segments.add(part);
            }
        }
        return segments;
    }

    private Map<String, List<String>> extractHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> {
            List<String> values = Collections.list(request.getHeaders(name));
            headers.put(name.toLowerCase(Locale.ENGLISH), values);
        });
        return headers;
    }
}
