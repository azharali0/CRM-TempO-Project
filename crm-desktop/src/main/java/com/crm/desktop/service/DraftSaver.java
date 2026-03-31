package com.crm.desktop.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Auto-saves form drafts locally every 30 seconds.
 * Drafts are stored in memory only — lost on app close (no disk persistence
 * to avoid leaving PII on the filesystem).
 */
public class DraftSaver {

    private static final Logger log = Logger.getLogger(DraftSaver.class.getName());
    private static final int SAVE_INTERVAL_SECONDS = 30;
    private static DraftSaver instance;

    private final Map<String, Map<String, String>> drafts = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    private DraftSaver() {}

    public static synchronized DraftSaver getInstance() {
        if (instance == null) {
            instance = new DraftSaver();
        }
        return instance;
    }

    /**
     * Starts the periodic save scheduler.
     */
    public void start() {
        stop();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "draft-saver");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::onTick, SAVE_INTERVAL_SECONDS,
                SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.fine("DraftSaver started — interval " + SAVE_INTERVAL_SECONDS + "s");
    }

    /**
     * Stops the periodic save scheduler.
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Saves a draft for a specific form identified by formKey.
     * Called from controllers on field changes.
     *
     * @param formKey unique key like "customer-form", "task-form:uuid"
     * @param fieldValues map of field name → current value
     */
    public void saveDraft(String formKey, Map<String, String> fieldValues) {
        if (formKey == null || fieldValues == null) return;
        drafts.put(formKey, new ConcurrentHashMap<>(fieldValues));
    }

    /**
     * Retrieves a saved draft for the given form key.
     *
     * @param formKey unique form identifier
     * @return saved field values, or null if no draft exists
     */
    public Map<String, String> getDraft(String formKey) {
        return drafts.get(formKey);
    }

    /**
     * Removes a draft after successful save.
     */
    public void clearDraft(String formKey) {
        drafts.remove(formKey);
    }

    /**
     * Clears all drafts (e.g., on logout).
     */
    public void clearAll() {
        drafts.clear();
    }

    /**
     * Returns true if a draft exists for the given key.
     */
    public boolean hasDraft(String formKey) {
        return drafts.containsKey(formKey);
    }

    private void onTick() {
        // The actual saving is triggered by controllers calling saveDraft().
        // This tick ensures any pending in-memory data is retained and could
        // be extended for disk persistence if needed in the future.
        if (!drafts.isEmpty()) {
            log.fine("DraftSaver tick — " + drafts.size() + " drafts in memory");
        }
    }
}
