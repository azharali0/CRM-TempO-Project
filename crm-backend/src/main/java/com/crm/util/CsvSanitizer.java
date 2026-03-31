package com.crm.util;

/**
 * Sanitizes CSV/Excel cell values to prevent CSV injection attacks.
 * Cells starting with =, +, -, @, |, tab, or carriage return are stripped
 * to prevent formula injection when the data is exported or displayed.
 */
public final class CsvSanitizer {

    private CsvSanitizer() {
    }

    /**
     * Sanitizes a cell value by stripping dangerous formula prefixes.
     *
     * @param value the raw cell value
     * @return sanitized value
     */
    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        // Strip leading characters that could be interpreted as formulas
        while (!trimmed.isEmpty() && isDangerousPrefix(trimmed.charAt(0))) {
            trimmed = trimmed.substring(1).trim();
        }

        // Also strip %0D (URL-encoded carriage return) at the start
        while (trimmed.startsWith("%0D") || trimmed.startsWith("%0d")) {
            trimmed = trimmed.substring(3).trim();
        }

        return trimmed;
    }

    private static boolean isDangerousPrefix(char c) {
        return c == '=' || c == '+' || c == '-' || c == '@' || c == '|' || c == '\t' || c == '\r';
    }
}
