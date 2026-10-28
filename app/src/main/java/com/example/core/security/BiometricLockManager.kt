package com.example.core.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class AuthPromptState {
    IDLE,
    SHOWING,
    AUTHENTICATED,
    ERROR
}

object BiometricLockManager {

    @Volatile
    private var isPromptActive = false

    var currentState: AuthPromptState = AuthPromptState.IDLE
        private set

    fun isPromptActive(): Boolean = isPromptActive

    fun resetState() {
        isPromptActive = false
        currentState = AuthPromptState.IDLE
    }

    enum class AuthCapability {
        READY,
        NO_HARDWARE,
        HW_UNAVAILABLE,
        NONE_ENROLLED,
        UNSUPPORTED
    }

    fun checkAuthCapability(context: Context): AuthCapability {
        val biometricManager = BiometricManager.from(context)
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            return when (biometricManager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> AuthCapability.READY
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> AuthCapability.NONE_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    if (keyguardManager?.isDeviceSecure == true) AuthCapability.READY else AuthCapability.NONE_ENROLLED
                }
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    if (keyguardManager?.isDeviceSecure == true) AuthCapability.READY else AuthCapability.HW_UNAVAILABLE
                }
                else -> if (keyguardManager?.isDeviceSecure == true) AuthCapability.READY else AuthCapability.UNSUPPORTED
            }
        } else {
            // Android 10 (API 29) compatibility
            val hasBiometric = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
            val isDeviceSecure = keyguardManager?.isDeviceSecure == true
            return if (hasBiometric || isDeviceSecure) {
                AuthCapability.READY
            } else if (keyguardManager?.isKeyguardSecure == false) {
                AuthCapability.NONE_ENROLLED
            } else {
                AuthCapability.NONE_ENROLLED
            }
        }
    }

    fun canAuthenticate(context: Context): Boolean {
        return checkAuthCapability(context) == AuthCapability.READY
    }

    fun showPrompt(
        activity: FragmentActivity,
        title: String = "Unlock Steadfast",
        subtitle: String = "Confirm your identity to access your private recovery data",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isPromptActive) return
        isPromptActive = true

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isPromptActive = false
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    isPromptActive = false
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Biometric failed a single attempt, prompt remains open
                }
            }
        )

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            promptInfoBuilder.setDeviceCredentialAllowed(true)
        }

        try {
            prompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            isPromptActive = false
            onError(e.message ?: "Authentication failed to launch")
        }
    }

    fun resetPromptState() {
        isPromptActive = false
    }
}
