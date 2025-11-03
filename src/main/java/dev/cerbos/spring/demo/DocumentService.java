package dev.cerbos.spring.demo;

import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DocumentService {

    private final DocumentRepository repository;

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    @PreAuthorize("@cerbosMethodAuthorizer.canViewDocuments(authentication)")
    @PostAuthorize("@cerbosMethodAuthorizer.canAccessDocument(authentication, returnObject, 'read')")
    public Document readDocument(String documentId) {
        return repository.findById(documentId).orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    @PreAuthorize("@cerbosMethodAuthorizer.canCreateDocument(authentication, #request)")
    public Document createDocument(@P("request") DocumentRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String owner = authentication != null ? authentication.getName() : "system";
        String documentId = StringUtils.hasText(request.getDocumentId())
                ? request.getDocumentId()
                : UUID.randomUUID().toString();
        Map<String, Object> content = request.getContent() != null ? Map.copyOf(request.getContent()) : Map.of();
        Document document = new Document(documentId, owner, content);
        return repository.save(document);
    }
}
