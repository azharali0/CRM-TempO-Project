package com.crm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputSanitizerTest {

    @Test
    void sanitize_stripsHtmlTags() {
        assertEquals("Hello World", InputSanitizer.sanitize("Hello <b>World</b>"));
    }

    @Test
    void sanitize_stripsScriptTags() {
        assertEquals("alert('xss')", InputSanitizer.sanitize("<script>alert('xss')</script>"));
    }

    @Test
    void sanitize_preservesPlainText() {
        assertEquals("Plain text", InputSanitizer.sanitize("Plain text"));
    }

    @Test
    void sanitize_returnsNullForNull() {
        assertNull(InputSanitizer.sanitize(null));
    }

    @Test
    void sanitize_trimsWhitespace() {
        assertEquals("trimmed", InputSanitizer.sanitize("  trimmed  "));
    }

    @Test
    void sanitize_stripsNestedTags() {
        assertEquals("content", InputSanitizer.sanitize("<div><p>content</p></div>"));
    }

    @Test
    void sanitizeOrNull_returnsNullForBlank() {
        assertNull(InputSanitizer.sanitizeOrNull("   "));
    }

    @Test
    void sanitizeOrNull_returnsNullForEmpty() {
        assertNull(InputSanitizer.sanitizeOrNull(""));
    }

    @Test
    void sanitizeOrNull_returnsValueForNonBlank() {
        assertEquals("value", InputSanitizer.sanitizeOrNull("value"));
    }

    @Test
    void sanitize_handlesComplexXss() {
        String xss = "<img src=x onerror=alert(1)>Hello";
        String result = InputSanitizer.sanitize(xss);
        assertFalse(result.contains("<"));
        assertFalse(result.contains(">"));
    }
}
