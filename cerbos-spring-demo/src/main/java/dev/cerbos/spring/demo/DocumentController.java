package dev.cerbos.spring.demo;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    @GetMapping("/{documentId}")
    public Map<String, Object> readDocument(@PathVariable String documentId, Authentication authentication) {
        return Map.of(
                "documentId", documentId,
                "reader", authentication.getName(),
                "timestamp", Instant.now().toString());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createDocument(
            @RequestBody Map<String, Object> request, Authentication authentication) {
        Map<String, Object> response =
                Map.of(
                        "documentId", request.getOrDefault("documentId", "generated"),
                        "owner", authentication.getName(),
                        "status", "CREATED");
        return ResponseEntity.ok(response);
    }
}
