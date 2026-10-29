package com.steadfast.app.core.security

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.data.datastore.SteadfastPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BiometricPromptPolicyTest {
    @Test @Config(sdk = [29])
    fun `Android ten builds supported weak biometric or credential prompt`() {
        val info = BiometricLockManager.promptInfo()
        assertEquals(0x80ff, info.allowedAuthenticators)
        assertTrue(info.negativeButtonText.isNullOrEmpty())
    }

    @Test @Config(sdk = [34])
    fun `modern Android builds strong biometric or credential prompt`() {
        assertEquals(0x800f, BiometricLockManager.promptInfo().allowedAuthenticators)
    }

    @Test @Config(sdk = [29])
    fun `credential only Android ten device is supported`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        org.robolectric.shadow.api.Shadow.extract<org.robolectric.shadows.ShadowBiometricManager>(context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)).setCanAuthenticate(false)
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        shadowOf(keyguard).setIsDeviceSecure(true)
        assertEquals(BiometricLockManager.AuthCapability.READY, BiometricLockManager.checkAuthCapability(context))
    }

    @Test @Config(sdk = [29])
    fun `unavailable lock enable rejected before preference write`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        org.robolectric.shadow.api.Shadow.extract<org.robolectric.shadows.ShadowBiometricManager>(context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)).setCanAuthenticate(false)
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        shadowOf(keyguard).setIsDeviceSecure(false)
        val preferences = SteadfastPreferences(context)
        preferences.setAppLockEnabled(false)
        assertFalse(BiometricLockManager.setAppLockEnabled(context, preferences, true))
        assertFalse(preferences.isAppLockEnabled.first())
        assertTrue(BiometricLockManager.setAppLockEnabled(context, preferences, false))
    }
}
