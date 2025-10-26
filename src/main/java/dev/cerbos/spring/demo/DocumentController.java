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
    public Map<String, Object> read(
            @PathVariable("documentId") String documentId, Authentication authentication) {
        return Map.of(
                "documentId", documentId,
                "action", "read",
                "performedBy", authentication.getName(),
                "timestamp", Instant.now().toString());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, Object> payload, Authentication authentication) {
        Map<String, Object> response = Map.of(
                "documentId", payload.getOrDefault("documentId", "generated"),
                "action", "create",
                "performedBy", authentication.getName());
        return ResponseEntity.ok(response);
    }
}
