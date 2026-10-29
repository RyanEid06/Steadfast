package com.steadfast.app.core.security

import org.junit.Assert.*
import org.junit.Test

class AppLockSessionTest {
    @Test fun `protected content waits for preferences`() {
        val session = AppLockSession()
        assertFalse(session.canShowProtectedContent)
        session.updatePreferences(false)
        assertTrue(session.canShowProtectedContent)
    }

    @Test fun `unavailable authentication and cancellation never unlock`() {
        val session = AppLockSession()
        val host = session.attachHost()
        session.updatePreferences(true)
        assertNull(session.beginPrompt(false))
        assertTrue(session.isLocked)
        assertFalse(session.canShowProtectedContent)
        val request = session.beginPrompt(true)!!
        session.failPrompt(host, request, "Cancelled")
        assertTrue(session.isLocked)
        assertFalse(session.takeAutomaticPrompt())
        assertEquals("Cancelled", session.errorMessage)
    }

    @Test fun `concurrent prompt request suppressed until terminal callback`() {
        val session = AppLockSession()
        val host = session.attachHost()
        session.updatePreferences(true)
        val request = session.beginPrompt(true)!!
        assertNull(session.beginPrompt(true))
        assertTrue(session.promptInFlight)
        assertTrue(session.isLocked)
        session.succeedPrompt(host, request)
        assertFalse(session.promptInFlight)
        assertTrue(session.canShowProtectedContent)
    }

    @Test fun `recreation retains prompt and rejects callbacks from replaced host`() {
        val session = AppLockSession()
        val oldHost = session.attachHost()
        session.updatePreferences(true)
        assertTrue(session.takeAutomaticPrompt())
        val request = session.beginPrompt(true)!!
        val newHost = session.attachHost()
        assertNull(session.beginPrompt(true))
        assertFalse(session.takeAutomaticPrompt())
        session.succeedPrompt(oldHost, request)
        assertTrue(session.isLocked)
        session.succeedPrompt(newHost, request)
        assertTrue(session.canShowProtectedContent)
    }

    @Test fun `stale callback from cancelled request cannot satisfy retry`() {
        val session = AppLockSession()
        val host = session.attachHost()
        session.updatePreferences(true)
        val oldRequest = session.beginPrompt(true)!!
        session.failPrompt(host, oldRequest, "Cancelled")
        val retry = session.beginPrompt(true)!!
        session.succeedPrompt(host, oldRequest)
        assertTrue(session.isLocked)
        assertTrue(session.promptInFlight)
        session.succeedPrompt(host, retry)
        assertTrue(session.canShowProtectedContent)
    }

    @Test fun `background content waits for resume and locks at thirty seconds`() {
        val session = unlockedSession()
        session.onBackground(100_000L)
        assertFalse(session.canShowProtectedContent)
        session.onForeground(129_999L, 100_000L)
        assertTrue(session.canShowProtectedContent)
        session.onBackground(200_000L)
        session.onForeground(230_000L, 200_000L)
        assertTrue(session.isLocked)
        assertFalse(session.canShowProtectedContent)
        assertTrue(session.takeAutomaticPrompt())
    }

    @Test fun `clock rollback fails closed and fresh process never restores authenticated state`() {
        val session = unlockedSession()
        session.onBackground(100_000L)
        session.onForeground(99_000L, 100_000L)
        assertTrue(session.isLocked)
        val freshProcess = AppLockSession()
        freshProcess.updatePreferences(true)
        assertFalse(freshProcess.canShowProtectedContent)
    }

    @Test fun `disabling lock invalidates outstanding prompt callback`() {
        val session = AppLockSession()
        val host = session.attachHost()
        session.updatePreferences(true)
        val oldRequest = session.beginPrompt(true)!!
        session.updatePreferences(false)
        session.updatePreferences(true)
        session.succeedPrompt(host, oldRequest)
        assertTrue(session.isLocked)
    }

    private fun unlockedSession(): AppLockSession = AppLockSession().also {
        val host = it.attachHost()
        it.updatePreferences(true)
        it.succeedPrompt(host, it.beginPrompt(true)!!)
    }
}
