package com.crm.util;

public final class InputSanitizer {

    private InputSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return stripHtmlTags(input).trim();
    }

    public static String sanitizeOrNull(String input) {
        if (input == null) {
            return null;
        }
        String sanitized = sanitize(input);
        return (sanitized == null || sanitized.isBlank()) ? null : sanitized;
    }

    private static String stripHtmlTags(String input) {
        StringBuilder result = new StringBuilder(input.length());
        boolean insideTag = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '<') {
                insideTag = true;
            } else if (c == '>') {
                insideTag = false;
            } else if (!insideTag) {
                result.append(c);
            }
        }
        return result.toString();
    }
}
