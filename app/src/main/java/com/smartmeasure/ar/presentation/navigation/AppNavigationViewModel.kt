package com.smartmeasure.ar.presentation.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Destination {
    DIAGNOSTICS,
    MANUAL_MEASUREMENT,
    AR_MEASUREMENT,
    FIELD_TRIALS,
}

/**
 * Screen navigation of the single-activity app. Survives activity recreation (rotation) as a
 * ViewModel and process death through [SavedStateHandle].
 *
 * Saved values are only read when a new instance is created, i.e. after process death: the ARCore
 * session and its anchors do not survive that, so an AR destination is restored as Diagnostics
 * (where the user can start AR again) instead of an AR screen without its session.
 */
class AppNavigationViewModel(
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _destination = MutableStateFlow(restore(KEY_DESTINATION))
    val destination: StateFlow<Destination> = _destination.asStateFlow()

    private var fieldTrialsReturn = restore(KEY_FIELD_TRIALS_RETURN)

    init {
        persist()
    }

    fun open(target: Destination) {
        _destination.value = target
        persist()
    }

    fun openFieldTrials(from: Destination) {
        fieldTrialsReturn = from
        open(Destination.FIELD_TRIALS)
    }

    /**
     * Handles a back press. Returns false on Diagnostics (the root): the press is left to the
     * system so the app closes normally.
     */
    fun back(): Boolean {
        val target = when (_destination.value) {
            Destination.DIAGNOSTICS -> return false
            Destination.FIELD_TRIALS -> fieldTrialsReturn
            Destination.MANUAL_MEASUREMENT, Destination.AR_MEASUREMENT -> Destination.DIAGNOSTICS
        }
        open(target)
        return true
    }

    private fun persist() {
        savedState[KEY_DESTINATION] = _destination.value.name
        savedState[KEY_FIELD_TRIALS_RETURN] = fieldTrialsReturn.name
    }

    private fun restore(key: String): Destination {
        val saved = savedState.get<String>(key)
            ?.let { name -> Destination.entries.firstOrNull { it.name == name } }
            ?: Destination.DIAGNOSTICS
        return if (saved == Destination.AR_MEASUREMENT) Destination.DIAGNOSTICS else saved
    }

    private companion object {
        const val KEY_DESTINATION = "destination"
        const val KEY_FIELD_TRIALS_RETURN = "field_trials_return"
    }
}
