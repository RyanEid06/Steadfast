package com.steadfast.app.core.security

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.steadfast.app.data.datastore.SteadfastPreferences

/** Retained for configuration changes only; a new process always starts unauthenticated. */
class AppLockSession : ViewModel() {
    var preferencesLoaded by mutableStateOf(false)
        private set
    var enabled by mutableStateOf(false)
        private set
    private var authenticated by mutableStateOf(false)
    var checkingResume by mutableStateOf(false)
        private set
    var activeRequest by mutableStateOf<Long?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    private var automaticPromptPending = false
    private var requestGeneration = 0L
    private var hostGeneration = 0L
    // null reads the persisted timestamp; zero means this session already consumed it.
    private var backgroundEpoch: Long? = null

    val promptInFlight: Boolean get() = activeRequest != null
    val isLocked: Boolean get() = preferencesLoaded && enabled && !authenticated
    val canShowProtectedContent: Boolean
        get() = preferencesLoaded && !checkingResume && (!enabled || authenticated)

    fun updatePreferences(lockEnabled: Boolean) {
        if (!preferencesLoaded || enabled != lockEnabled) {
            authenticated = false
            activeRequest = null
            requestGeneration++
            automaticPromptPending = lockEnabled
            errorMessage = null
        }
        enabled = lockEnabled
        preferencesLoaded = true
        if (!enabled) checkingResume = false
    }

    fun attachHost(): Long = ++hostGeneration

    fun takeAutomaticPrompt(): Boolean {
        if (!isLocked || checkingResume || !automaticPromptPending || promptInFlight) return false
        automaticPromptPending = false
        return true
    }

    @Synchronized
    fun beginPrompt(capabilityReady: Boolean): Long? {
        if (!isLocked || promptInFlight || checkingResume) return null
        automaticPromptPending = false
        if (!capabilityReady) {
            errorMessage = BiometricLockManager.ENROLLMENT_GUIDANCE
            return null
        }
        errorMessage = null
        return (++requestGeneration).also { activeRequest = it }
    }

    fun succeedPrompt(host: Long, request: Long) {
        if (host != hostGeneration || request != activeRequest || !enabled) return
        activeRequest = null
        authenticated = true
        checkingResume = false
        backgroundEpoch = 0L
        errorMessage = null
    }

    fun failPrompt(host: Long, request: Long, message: String) {
        if (host != hostGeneration || request != activeRequest) return
        activeRequest = null
        errorMessage = message
        // A terminal error or cancellation waits for explicit Unlock, including after rotation.
        automaticPromptPending = false
    }

    fun cancelPrompt(host: Long) {
        if (host != hostGeneration) return
        activeRequest = null
        requestGeneration++
        automaticPromptPending = false
    }

    fun onBackground(epochMillis: Long) {
        backgroundEpoch = epochMillis
        checkingResume = enabled
    }

    fun onForeground(now: Long, persistedBackgroundEpoch: Long) {
        val last = backgroundEpoch ?: persistedBackgroundEpoch
        backgroundEpoch = 0L
        if (enabled && authenticated && last != 0L && (now < last || now - last >= 30_000L)) {
            authenticated = false
            automaticPromptPending = true
            errorMessage = null
        }
        checkingResume = false
    }
}

object BiometricLockManager {
    const val ENROLLMENT_GUIDANCE = "Device authentication is unavailable. Set up or check your screen lock and biometrics in Android Settings, then try again."

    enum class AuthCapability { READY, NO_HARDWARE, HW_UNAVAILABLE, NONE_ENROLLED, UNSUPPORTED }

    private fun authenticators(): Int =
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        } else {
            // STRONG | DEVICE_CREDENTIAL is unsupported on Android 10.
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        }) or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun checkAuthCapability(context: Context): AuthCapability = try {
        when (BiometricManager.from(context).canAuthenticate(authenticators())) {
            BiometricManager.BIOMETRIC_SUCCESS -> AuthCapability.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> AuthCapability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> AuthCapability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> AuthCapability.HW_UNAVAILABLE
            else -> AuthCapability.UNSUPPORTED
        }
    } catch (_: Exception) {
        AuthCapability.UNSUPPORTED
    }

    fun canAuthenticate(context: Context): Boolean = checkAuthCapability(context) == AuthCapability.READY

    fun canChangeAppLock(context: Context, enabled: Boolean): Boolean {
        if (!enabled || canAuthenticate(context)) return true
        Toast.makeText(context, ENROLLMENT_GUIDANCE, Toast.LENGTH_LONG).show()
        return false
    }

    suspend fun setAppLockEnabled(context: Context, preferences: SteadfastPreferences, enabled: Boolean): Boolean {
        if (!canChangeAppLock(context, enabled)) return false
        preferences.setAppLockEnabled(enabled)
        return true
    }

    fun promptInfo(): BiometricPrompt.PromptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Steadfast")
        .setSubtitle("Verify to access your private recovery data")
        .setAllowedAuthenticators(authenticators())
        .build()

    /** AndroidX retains its prompt fragment; recreate the client to replace its callback, not its dialog. */
    class PromptController(private val activity: FragmentActivity, private val session: AppLockSession) {
        private val host = session.attachHost()
        private var prompt: BiometricPrompt? = session.activeRequest?.let { bind(it) }

        private fun bind(request: Long): BiometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            callback(session, host, request)
        )

        fun showPrompt(): Boolean {
            val request = session.beginPrompt(canAuthenticate(activity)) ?: return false
            return try {
                prompt = bind(request)
                prompt!!.authenticate(promptInfo())
                true
            } catch (error: Exception) {
                session.failPrompt(host, request, error.message ?: "Verification could not start. Please try again.")
                false
            }
        }

        fun dispose(changingConfigurations: Boolean) {
            if (!changingConfigurations) {
                session.cancelPrompt(host)
                prompt?.cancelAuthentication()
            }
        }
    }

    // Callbacks retain only the session; request/host checks reject callbacks from replaced clients.
    private fun callback(session: AppLockSession, host: Long, request: Long) =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                session.succeedPrompt(host, request)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                session.failPrompt(host, request, errString.toString())
            }

            override fun onAuthenticationFailed() {
                // A rejected biometric attempt is nonterminal; the system dialog remains active.
            }
        }
}
