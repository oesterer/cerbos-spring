package dev.cerbos.spring.demo;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentRepository {

    private final Map<String, Document> documents = new ConcurrentHashMap<>();

    public Optional<Document> findById(String documentId) {
        return Optional.ofNullable(documents.get(documentId));
    }

    public Document save(Document document) {
        documents.put(document.id(), document);
        return document;
    }

    @PostConstruct
    void init() {
        documents.put("alpha", new Document("alpha", "alice", Map.of("title", "Quarterly plan")));
        documents.put("beta", new Document("beta", "bob", Map.of("title", "Draft budget")));
    }
}
