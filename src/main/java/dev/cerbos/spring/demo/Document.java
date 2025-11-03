package dev.cerbos.spring.demo;

import java.util.Map;

public record Document(String id, String owner, Map<String, Object> content) {
}
