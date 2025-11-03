package dev.cerbos.spring.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{documentId}")
    public Document read(@PathVariable("documentId") String documentId) {
        return documentService.readDocument(documentId);
    }

    @PostMapping
    public ResponseEntity<Document> create(@RequestBody DocumentRequest request) {
        Document document = documentService.createDocument(request);
        return ResponseEntity.status(201).body(document);
    }
}
