package com.steadfast.app.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.atomic.AtomicBoolean

enum class AuthPromptState {
    IDLE,
    PROMPT_IN_FLIGHT,
    AUTHENTICATED,
    FAILED_OR_CANCELLED
}

object BiometricLockManager {

    private val isPromptInFlight = AtomicBoolean(false)
    var currentState: AuthPromptState = AuthPromptState.IDLE
        private set

    fun canAuthenticate(activity: FragmentActivity): Boolean {
        val biometricManager = BiometricManager.from(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Attempts to show the BiometricPrompt.
     * Returns true if a prompt was successfully launched, or false if a prompt was
     * already in-flight or device cannot authenticate.
     */
    @Synchronized
    fun showPrompt(
        activity: FragmentActivity,
        title: String = "Unlock Steadfast",
        subtitle: String = "Confirm your identity to access your private recovery data",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        if (!canAuthenticate(activity)) {
            currentState = AuthPromptState.AUTHENTICATED
            onSuccess()
            return false
        }

        // Guard: Maximum one active authentication prompt in flight
        if (isPromptInFlight.get()) {
            return false
        }

        if (!isPromptInFlight.compareAndSet(false, true)) {
            return false
        }
        currentState = AuthPromptState.PROMPT_IN_FLIGHT

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isPromptInFlight.set(false)
                    currentState = AuthPromptState.AUTHENTICATED
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    isPromptInFlight.set(false)
                    currentState = AuthPromptState.FAILED_OR_CANCELLED
                    // If user cancels or fails, stay locked safely without looping
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Biometric sensor rejected credential, but dialog stays up until user retries or cancels.
                    // inFlight remains true.
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        return try {
            prompt.authenticate(promptInfo)
            true
        } catch (e: Exception) {
            isPromptInFlight.set(false)
            currentState = AuthPromptState.FAILED_OR_CANCELLED
            onError(e.message ?: "Authentication initialization failed")
            false
        }
    }

    /**
     * Resets the in-flight state (e.g. on activity destruction or when test harness resets).
     */
    fun resetState() {
        isPromptInFlight.set(false)
        currentState = AuthPromptState.IDLE
    }

    fun isPromptActive(): Boolean = isPromptInFlight.get()
}
