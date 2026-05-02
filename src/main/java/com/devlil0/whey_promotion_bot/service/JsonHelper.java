package com.devlil0.whey_promotion_bot.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public final class JsonHelper {

    private JsonHelper() {}

    public static String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) return null;
        return node.get(field).asText(null);
    }

    public static Integer integer(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) return null;
        if (node.get(field).isNumber()) return node.get(field).asInt();
        try {
            String value = node.get(field).asText();
            if (value == null || value.isBlank()) return null;
            return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    public static Long longValue(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) return null;
        if (node.get(field).isNumber()) return node.get(field).asLong();
        try {
            String value = node.get(field).asText();
            if (value == null || value.isBlank()) return null;
            return Long.parseLong(value.replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    public static BigDecimal decimal(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) return null;
        if (node.get(field).isNumber()) {
            return node.get(field).decimalValue();
        }
        return decimalFromText(node.get(field).asText());
    }

    public static BigDecimal decimalFromText(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String normalized = value.replace("R$", "").trim();

            // Formato BR: 1.234,56
            if (normalized.contains(",")) {
                normalized = normalized.replace(".", "").replace(",", ".");
            }

            return new BigDecimal(normalized);
        } catch (Exception e) {
            return null;
        }
    }
}
