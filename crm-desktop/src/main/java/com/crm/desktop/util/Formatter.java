package com.crm.desktop.util;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Number and date formatting utilities for the desktop UI.
 */
public final class Formatter {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DISPLAY_DATETIME =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter API_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.US);

    private Formatter() {}

    /**
     * Formats a date string from the API (ISO) to a human-readable form.
     */
    public static String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return "";
        try {
            // Try datetime first
            LocalDateTime dt = LocalDateTime.parse(isoDate.substring(0,
                    Math.min(isoDate.length(), 19)), API_DATETIME);
            return dt.format(DISPLAY_DATETIME);
        } catch (DateTimeParseException e) {
            try {
                LocalDate d = LocalDate.parse(isoDate.substring(0, 10));
                return d.format(DISPLAY_DATE);
            } catch (DateTimeParseException e2) {
                return isoDate;
            }
        }
    }

    /**
     * Formats a number as currency ($12,345.00).
     */
    public static String formatCurrency(double amount) {
        return CURRENCY.format(amount);
    }

    /**
     * Formats a number as currency from string.
     */
    public static String formatCurrency(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) return "$0.00";
        try {
            return formatCurrency(Double.parseDouble(amountStr));
        } catch (NumberFormatException e) {
            return "$0.00";
        }
    }

    /**
     * Formats a number with commas (1,234,567).
     */
    public static String formatNumber(long number) {
        return NumberFormat.getNumberInstance(Locale.US).format(number);
    }

    /**
     * Converts minutes to "Xh Ym" format.
     */
    public static String formatDuration(int minutes) {
        if (minutes < 60) return minutes + "m";
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}
