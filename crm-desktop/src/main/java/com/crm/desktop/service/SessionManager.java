package com.crm.desktop.service;

/**
 * Stores JWT tokens in memory ONLY — never writes to disk.
 * Tracks last activity time for 15-minute idle auto-logout.
 */
public class SessionManager {

    private static final long IDLE_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes
    private static SessionManager instance;

    private String accessToken;
    private String refreshToken;
    private String userName;
    private String userEmail;
    private String userRole;
    private long lastActivityTime;

    private SessionManager() {
        clearSession();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Stores authentication data after successful login.
     * Tokens are kept only in Java memory — never serialized.
     */
    public void setSession(String accessToken, String refreshToken,
                           String userName, String userEmail, String userRole) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userRole = userRole;
        this.lastActivityTime = System.currentTimeMillis();
    }

    /**
     * Clears all session data. Called on logout or timeout.
     */
    public void clearSession() {
        this.accessToken = null;
        this.refreshToken = null;
        this.userName = null;
        this.userEmail = null;
        this.userRole = null;
        this.lastActivityTime = 0;
    }

    public boolean isLoggedIn() {
        return accessToken != null;
    }

    /**
     * Checks whether the user has been idle for more than 15 minutes.
     */
    public boolean isSessionExpired() {
        if (!isLoggedIn()) return true;
        return (System.currentTimeMillis() - lastActivityTime) > IDLE_TIMEOUT_MS;
    }

    /**
     * Called on every user interaction to reset the idle timer.
     */
    public void recordActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    public void updateAccessToken(String newAccessToken) {
        this.accessToken = newAccessToken;
        recordActivity();
    }

    // ── Getters ────────────────────────────────────────

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserRole() {
        return userRole;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(userRole);
    }

    public boolean isManager() {
        return "MANAGER".equals(userRole);
    }

    public boolean isSalesRep() {
        return "SALES_REP".equals(userRole);
    }
}
