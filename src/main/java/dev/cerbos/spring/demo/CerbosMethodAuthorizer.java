package dev.cerbos.spring.demo;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CerbosMethodAuthorizer {

    private static final String DOCUMENT_KIND = "document";

    private final CerbosAuthorizationService authorizationService;

    public CerbosMethodAuthorizer(CerbosAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public boolean canViewDocuments(Authentication authentication) {
        return authorizationService.checkPermission(
                authentication, DOCUMENT_KIND, "_ANY_", "read", Map.of(), Map.of());
    }

    public boolean canAccessDocument(Authentication authentication, Document document, String action) {
        if (document == null) {
            return false;
        }
        Map<String, Object> resourceAttributes = new LinkedHashMap<>();
        resourceAttributes.put("owner", document.owner());
        resourceAttributes.put("content", document.content());
        return authorizationService.checkPermission(
                authentication, DOCUMENT_KIND, document.id(), action, resourceAttributes, Map.of());
    }

    public boolean canCreateDocument(Authentication authentication, DocumentRequest request) {
        Map<String, Object> resourceAttributes = new LinkedHashMap<>();
        if (request.getContent() != null && !request.getContent().isEmpty()) {
            resourceAttributes.put("content", request.getContent());
        }
        if (StringUtils.hasText(request.getDocumentId())) {
            resourceAttributes.put("requestedId", request.getDocumentId());
        }
        String resourceId = StringUtils.hasText(request.getDocumentId()) ? request.getDocumentId() : "_NEW_";
        Map<String, Object> principalAttributes = Map.of("requestedOwner", authentication != null ? authentication.getName() : "system");
        return authorizationService.checkPermission(
                authentication, DOCUMENT_KIND, resourceId, "create", resourceAttributes, principalAttributes);
    }
}
