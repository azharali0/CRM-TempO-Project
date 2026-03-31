package com.crm.desktop.util;

import java.util.regex.Pattern;

/**
 * Client-side input validation.
 * Provides quick feedback before sending requests to the server.
 */
public final class Validator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9+\\-()\\s]{7,20}$");

    private Validator() {}

    public static boolean isNullOrBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean isValidName(String name) {
        return name != null && !name.isBlank() && name.length() <= 100;
    }

    /**
     * Validates that a string represents a non-negative decimal number.
     */
    public static boolean isValidDecimal(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            double d = Double.parseDouble(value);
            return d >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
