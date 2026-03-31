package com.crm.desktop;

import com.crm.desktop.service.DraftSaver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DraftSaver service.
 */
class DraftSaverTest {

    private DraftSaver draftSaver;

    @BeforeEach
    void setUp() {
        draftSaver = DraftSaver.getInstance();
        draftSaver.clearAll();
    }

    @Test
    void saveDraft_storesFieldValues() {
        Map<String, String> fields = new HashMap<>();
        fields.put("name", "Acme Corp");
        fields.put("email", "contact@acme.com");

        draftSaver.saveDraft("customer-form", fields);

        Map<String, String> retrieved = draftSaver.getDraft("customer-form");
        assertNotNull(retrieved);
        assertEquals("Acme Corp", retrieved.get("name"));
        assertEquals("contact@acme.com", retrieved.get("email"));
    }

    @Test
    void getDraft_returnsNullWhenNoDraft() {
        assertNull(draftSaver.getDraft("nonexistent-form"));
    }

    @Test
    void clearDraft_removesSpecificDraft() {
        Map<String, String> fields = new HashMap<>();
        fields.put("title", "Follow up");
        draftSaver.saveDraft("task-form:abc", fields);
        assertTrue(draftSaver.hasDraft("task-form:abc"));

        draftSaver.clearDraft("task-form:abc");
        assertFalse(draftSaver.hasDraft("task-form:abc"));
    }

    @Test
    void clearAll_removesAllDrafts() {
        draftSaver.saveDraft("form-a", Map.of("x", "1"));
        draftSaver.saveDraft("form-b", Map.of("y", "2"));
        assertTrue(draftSaver.hasDraft("form-a"));
        assertTrue(draftSaver.hasDraft("form-b"));

        draftSaver.clearAll();
        assertFalse(draftSaver.hasDraft("form-a"));
        assertFalse(draftSaver.hasDraft("form-b"));
    }

    @Test
    void saveDraft_overwritesPreviousDraft() {
        draftSaver.saveDraft("my-form", Map.of("name", "Old"));
        draftSaver.saveDraft("my-form", Map.of("name", "New"));

        assertEquals("New", draftSaver.getDraft("my-form").get("name"));
    }

    @Test
    void saveDraft_ignoresNullInputs() {
        draftSaver.saveDraft(null, Map.of("a", "b"));
        draftSaver.saveDraft("key", null);
        // Should not throw
    }

    @Test
    void hasDraft_returnsFalseForMissing() {
        assertFalse(draftSaver.hasDraft("does-not-exist"));
    }

    @Test
    void startAndStop_doNotThrow() {
        assertDoesNotThrow(() -> {
            draftSaver.start();
            draftSaver.stop();
        });
    }
}
