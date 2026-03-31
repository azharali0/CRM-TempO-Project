package com.crm.desktop;

import com.crm.desktop.util.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest {

    @Test
    void isNullOrBlank_nullReturnsTrue() {
        assertTrue(Validator.isNullOrBlank(null));
    }

    @Test
    void isNullOrBlank_emptyReturnsTrue() {
        assertTrue(Validator.isNullOrBlank(""));
    }

    @Test
    void isNullOrBlank_blankReturnsTrue() {
        assertTrue(Validator.isNullOrBlank("   "));
    }

    @Test
    void isNullOrBlank_valueReturnsFalse() {
        assertFalse(Validator.isNullOrBlank("hello"));
    }

    @Test
    void isValidEmail_validEmails() {
        assertTrue(Validator.isValidEmail("user@example.com"));
        assertTrue(Validator.isValidEmail("a.b@company.co.uk"));
        assertTrue(Validator.isValidEmail("test+tag@gmail.com"));
    }

    @Test
    void isValidEmail_invalidEmails() {
        assertFalse(Validator.isValidEmail(null));
        assertFalse(Validator.isValidEmail(""));
        assertFalse(Validator.isValidEmail("notanemail"));
        assertFalse(Validator.isValidEmail("@nodomain"));
        assertFalse(Validator.isValidEmail("user@"));
    }

    @Test
    void isValidPhone_validPhones() {
        assertTrue(Validator.isValidPhone("0300-1234567"));
        assertTrue(Validator.isValidPhone("+1 (555) 123-4567"));
        assertTrue(Validator.isValidPhone("1234567890"));
    }

    @Test
    void isValidPhone_invalidPhones() {
        assertFalse(Validator.isValidPhone(null));
        assertFalse(Validator.isValidPhone(""));
        assertFalse(Validator.isValidPhone("abc"));
        assertFalse(Validator.isValidPhone("12"));
    }

    @Test
    void isValidPassword_minLength8() {
        assertTrue(Validator.isValidPassword("password"));
        assertTrue(Validator.isValidPassword("12345678"));
        assertFalse(Validator.isValidPassword("short"));
        assertFalse(Validator.isValidPassword(null));
        assertFalse(Validator.isValidPassword("1234567"));
    }

    @Test
    void isValidName_valid() {
        assertTrue(Validator.isValidName("John Doe"));
        assertTrue(Validator.isValidName("A"));
    }

    @Test
    void isValidName_invalid() {
        assertFalse(Validator.isValidName(null));
        assertFalse(Validator.isValidName(""));
        assertFalse(Validator.isValidName("   "));
        assertFalse(Validator.isValidName("A".repeat(101)));
    }

    @Test
    void isValidDecimal_valid() {
        assertTrue(Validator.isValidDecimal("100.50"));
        assertTrue(Validator.isValidDecimal("0"));
        assertTrue(Validator.isValidDecimal("99999"));
    }

    @Test
    void isValidDecimal_invalid() {
        assertFalse(Validator.isValidDecimal(null));
        assertFalse(Validator.isValidDecimal(""));
        assertFalse(Validator.isValidDecimal("abc"));
        assertFalse(Validator.isValidDecimal("-5"));
    }
}
