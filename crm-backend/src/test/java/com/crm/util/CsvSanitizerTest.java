package com.crm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CsvSanitizerTest {

    @Test
    void sanitize_stripsEqualsPrefix() {
        assertEquals("CMD()", CsvSanitizer.sanitize("=CMD()"));
    }

    @Test
    void sanitize_stripsPlusPrefix() {
        assertEquals("CMD()", CsvSanitizer.sanitize("+CMD()"));
    }

    @Test
    void sanitize_stripsMinusPrefix() {
        assertEquals("CMD()", CsvSanitizer.sanitize("-CMD()"));
    }

    @Test
    void sanitize_stripsAtPrefix() {
        assertEquals("SUM(A1:A10)", CsvSanitizer.sanitize("@SUM(A1:A10)"));
    }

    @Test
    void sanitize_stripsPipePrefix() {
        assertEquals("command", CsvSanitizer.sanitize("|command"));
    }

    @Test
    void sanitize_stripsTabPrefix() {
        assertEquals("data", CsvSanitizer.sanitize("\tdata"));
    }

    @Test
    void sanitize_stripsMultipleDangerousPrefixes() {
        assertEquals("CMD()", CsvSanitizer.sanitize("=+CMD()"));
    }

    @Test
    void sanitize_preservesNormalText() {
        assertEquals("Normal text", CsvSanitizer.sanitize("Normal text"));
    }

    @Test
    void sanitize_returnsNullForNull() {
        assertNull(CsvSanitizer.sanitize(null));
    }

    @Test
    void sanitize_returnsEmptyForEmpty() {
        assertEquals("", CsvSanitizer.sanitize(""));
    }

    @Test
    void sanitize_stripsUrlEncodedCarriageReturn() {
        assertEquals("data", CsvSanitizer.sanitize("%0Ddata"));
    }

    @Test
    void sanitize_handlesOnlyDangerousChars() {
        assertEquals("", CsvSanitizer.sanitize("=+-@|"));
    }
}
