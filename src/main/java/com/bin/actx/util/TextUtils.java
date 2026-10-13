package com.bin.actx.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Iterator;
import java.util.Map;

/**
 * class description
 *
 * @author Bin
 * @since 2026/9/14 13:20
 */
public class TextUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String limit(String str, int maxLength) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        String trimmed = str.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                JsonNode node = OBJECT_MAPPER.readTree(trimmed);
                truncateJsonNode(node, maxLength);
                return OBJECT_MAPPER.writeValueAsString(node);
            } catch (JsonProcessingException ignored) {
                // not valid JSON, fall through to plain text limit
            }
        }

        if (str.length() > maxLength) {
            return str.substring(0, maxLength) + "...(Total %s char)".formatted(str.length());
        }
        return str;
    }

    private static void truncateJsonNode(JsonNode node, int maxLength) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                    String text = value.asText();
                    if (text.length() > maxLength) {
                        objectNode.set(entry.getKey(), 
                            new TextNode(text.substring(0, maxLength) + "...(Total %s char)".formatted(text.length())));
                    }
                } else if (value.isObject() || value.isArray()) {
                    truncateJsonNode(value, maxLength);
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode element = arrayNode.get(i);
                if (element.isTextual()) {
                    String text = element.asText();
                    if (text.length() > maxLength) {
                        arrayNode.set(i, 
                            new TextNode(text.substring(0, maxLength) + "...(Total %s char)".formatted(text.length())));
                    }
                } else if (element.isObject() || element.isArray()) {
                    truncateJsonNode(element, maxLength);
                }
            }
        }
    }

}
