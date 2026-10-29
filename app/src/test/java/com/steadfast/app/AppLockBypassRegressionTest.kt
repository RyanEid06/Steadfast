package com.steadfast.app

import android.app.KeyguardManager
import android.content.Context
import com.steadfast.app.core.security.AppLockSession
import com.steadfast.app.core.security.BiometricLockManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBiometricManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class AppLockBypassRegressionTest {
    @Test
    fun `unavailable authentication never unlocks protected content`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()
        val activity = controller.get()
        Shadow.extract<ShadowBiometricManager>(activity.getSystemService(android.hardware.biometrics.BiometricManager::class.java)).setCanAuthenticate(false)
        shadowOf(activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).setIsDeviceSecure(false)
        assertFalse("Fixture must have no usable authentication", BiometricLockManager.canAuthenticate(activity))
        val session = AppLockSession()
        session.updatePreferences(true)
        val prompt = BiometricLockManager.PromptController(activity, session)
        assertFalse(prompt.showPrompt())
        assertTrue("Unavailable authentication must keep the app locked", session.isLocked)
        assertFalse(session.canShowProtectedContent)
        assertNotNull(session.errorMessage)
        prompt.dispose(changingConfigurations = false)
        controller.destroy()
    }
}
