package com.crm.desktop;

import com.crm.desktop.util.Formatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatterTest {

    @Test
    void formatDate_isoDateTime() {
        String result = Formatter.formatDate("2026-03-15T10:30:00");
        assertTrue(result.contains("Mar"));
        assertTrue(result.contains("15"));
        assertTrue(result.contains("2026"));
    }

    @Test
    void formatDate_isoDateOnly() {
        String result = Formatter.formatDate("2026-03-15");
        assertTrue(result.contains("Mar"));
        assertTrue(result.contains("15"));
    }

    @Test
    void formatDate_nullOrEmpty() {
        assertEquals("", Formatter.formatDate(null));
        assertEquals("", Formatter.formatDate(""));
    }

    @Test
    void formatCurrency_double() {
        String result = Formatter.formatCurrency(12345.67);
        assertTrue(result.contains("12,345"));
    }

    @Test
    void formatCurrency_string() {
        String result = Formatter.formatCurrency("5000.00");
        assertTrue(result.contains("5,000"));
    }

    @Test
    void formatCurrency_invalidString() {
        assertEquals("$0.00", Formatter.formatCurrency("abc"));
        assertEquals("$0.00", Formatter.formatCurrency((String) null));
    }

    @Test
    void formatNumber_withCommas() {
        assertEquals("1,234,567", Formatter.formatNumber(1234567));
    }

    @Test
    void formatDuration_lessThanHour() {
        assertEquals("45m", Formatter.formatDuration(45));
    }

    @Test
    void formatDuration_moreThanHour() {
        assertEquals("2h 15m", Formatter.formatDuration(135));
    }
}
