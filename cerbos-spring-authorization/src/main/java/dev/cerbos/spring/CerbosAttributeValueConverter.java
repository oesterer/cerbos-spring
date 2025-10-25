package dev.cerbos.spring;

import dev.cerbos.sdk.builders.AttributeValue;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CerbosAttributeValueConverter {

    private CerbosAttributeValueConverter() {
    }

    static Map<String, AttributeValue> fromObjectMap(Map<String, Object> attributes) {
        Map<String, AttributeValue> converted = new LinkedHashMap<>();
        if (attributes == null) {
            return converted;
        }
        attributes.forEach((key, value) -> converted.put(key, fromObject(value)));
        return converted;
    }

    @SuppressWarnings("unchecked")
    static AttributeValue fromObject(Object value) {
        if (value == null) {
            return AttributeValue.stringValue("null");
        }
        if (value instanceof AttributeValue attrValue) {
            return attrValue;
        }
        if (value instanceof String stringValue) {
            return AttributeValue.stringValue(stringValue);
        }
        if (value instanceof Number numberValue) {
            return AttributeValue.doubleValue(numberValue.doubleValue());
        }
        if (value instanceof Boolean booleanValue) {
            return AttributeValue.boolValue(booleanValue);
        }
        if (value instanceof Enum<?> enumValue) {
            return AttributeValue.stringValue(enumValue.name());
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> stringKeyed = new LinkedHashMap<>();
            mapValue.forEach((k, v) -> {
                if (k != null) {
                    stringKeyed.put(String.valueOf(k), v);
                }
            });
            return AttributeValue.mapValue(fromObjectMap(stringKeyed));
        }
        if (value instanceof Collection<?> collectionValue) {
            List<AttributeValue> list = collectionValue.stream().map(CerbosAttributeValueConverter::fromObject).toList();
            return AttributeValue.listValue(list);
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<AttributeValue> list = java.util.stream.IntStream.range(0, length)
                    .mapToObj(index -> java.lang.reflect.Array.get(value, index))
                    .map(CerbosAttributeValueConverter::fromObject)
                    .toList();
            return AttributeValue.listValue(list);
        }
        return AttributeValue.stringValue(value.toString());
    }
}
