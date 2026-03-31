package com.crm.util;

public final class InputSanitizer {

    private static final String HTML_TAG_PATTERN = "<[^>]*>";

    private InputSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll(HTML_TAG_PATTERN, "").trim();
    }

    public static String sanitizeOrNull(String input) {
        if (input == null) {
            return null;
        }
        String sanitized = sanitize(input);
        return (sanitized == null || sanitized.isBlank()) ? null : sanitized;
    }
}
