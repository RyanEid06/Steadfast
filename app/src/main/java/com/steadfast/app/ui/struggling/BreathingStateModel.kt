package com.steadfast.app.ui.struggling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Configuration memory only: process restart deliberately begins a new inhale. */
class BreathingStateModel : ViewModel() {
    var phaseIndex by mutableIntStateOf(0)
        private set
    var secondsRemaining by mutableIntStateOf(BreathPhase.INHALE.seconds)
        private set

    fun advanceSecond() {
        if (secondsRemaining > 1) secondsRemaining--
        else {
            phaseIndex = (phaseIndex + 1) % BreathPhase.values().size
            secondsRemaining = BreathPhase.values()[phaseIndex].seconds
        }
    }
}
