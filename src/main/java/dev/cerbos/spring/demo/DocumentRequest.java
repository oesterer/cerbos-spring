package dev.cerbos.spring.demo;

import java.util.Collections;
import java.util.Map;

public class DocumentRequest {

    private String documentId;
    private Map<String, Object> content = Collections.emptyMap();

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public void setContent(Map<String, Object> content) {
        this.content = content != null ? content : Collections.emptyMap();
    }
}
