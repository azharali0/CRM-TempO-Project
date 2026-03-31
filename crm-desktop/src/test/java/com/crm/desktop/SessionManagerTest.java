package com.crm.desktop;

import com.crm.desktop.service.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    private SessionManager session;

    @BeforeEach
    void setUp() {
        session = SessionManager.getInstance();
        session.clearSession();
    }

    @Test
    void clearSession_clearsAllData() {
        session.setSession("access", "refresh", "John", "john@test.com", "ADMIN");
        session.clearSession();

        assertNull(session.getAccessToken());
        assertNull(session.getRefreshToken());
        assertNull(session.getUserName());
        assertNull(session.getUserEmail());
        assertNull(session.getUserRole());
        assertFalse(session.isLoggedIn());
    }

    @Test
    void setSession_storesTokensInMemory() {
        session.setSession("access123", "refresh456", "Alice", "alice@test.com", "MANAGER");

        assertEquals("access123", session.getAccessToken());
        assertEquals("refresh456", session.getRefreshToken());
        assertEquals("Alice", session.getUserName());
        assertEquals("alice@test.com", session.getUserEmail());
        assertEquals("MANAGER", session.getUserRole());
        assertTrue(session.isLoggedIn());
    }

    @Test
    void isAdmin_correctRoleCheck() {
        session.setSession("a", "r", "Admin", "admin@test.com", "ADMIN");
        assertTrue(session.isAdmin());
        assertFalse(session.isManager());
        assertFalse(session.isSalesRep());
    }

    @Test
    void isManager_correctRoleCheck() {
        session.setSession("a", "r", "Manager", "mgr@test.com", "MANAGER");
        assertFalse(session.isAdmin());
        assertTrue(session.isManager());
        assertFalse(session.isSalesRep());
    }

    @Test
    void isSalesRep_correctRoleCheck() {
        session.setSession("a", "r", "Sales", "sales@test.com", "SALES_REP");
        assertFalse(session.isAdmin());
        assertFalse(session.isManager());
        assertTrue(session.isSalesRep());
    }

    @Test
    void updateAccessToken_updatesTokenOnly() {
        session.setSession("old-access", "refresh", "User", "u@t.com", "ADMIN");
        session.updateAccessToken("new-access");

        assertEquals("new-access", session.getAccessToken());
        assertEquals("refresh", session.getRefreshToken());
    }

    @Test
    void isSessionExpired_falseWhenJustLoggedIn() {
        session.setSession("a", "r", "User", "u@t.com", "ADMIN");
        assertFalse(session.isSessionExpired());
    }

    @Test
    void isSessionExpired_trueWhenNotLoggedIn() {
        assertTrue(session.isSessionExpired());
    }

    @Test
    void recordActivity_resetsIdleTimer() {
        session.setSession("a", "r", "User", "u@t.com", "ADMIN");
        session.recordActivity();
        assertFalse(session.isSessionExpired());
    }

    @Test
    void singleton_returnsSameInstance() {
        SessionManager s1 = SessionManager.getInstance();
        SessionManager s2 = SessionManager.getInstance();
        assertSame(s1, s2);
    }
}
