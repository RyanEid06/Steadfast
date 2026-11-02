package com.steadfast.app.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Configuration changes retain the operation; process restart reconciles persisted setup. */
class OnboardingCompletionModel : ViewModel() {
    var isCompleting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun complete(action: suspend () -> Unit) {
        if (isCompleting) return
        isCompleting = true
        error = null
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Setup could not finish. Your history is preserved. Please retry." }
            finally { isCompleting = false }
        }
    }
}
