package com.smartmeasure.ar.presentation.navigation

import androidx.lifecycle.SavedStateHandle
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AppNavigationViewModelTest : StringSpec({
    "starts on Diagnostics" {
        AppNavigationViewModel(SavedStateHandle()).destination.value shouldBe Destination.DIAGNOSTICS
    }

    "back on a secondary screen returns to Diagnostics and is consumed" {
        val nav = AppNavigationViewModel(SavedStateHandle())
        nav.open(Destination.MANUAL_MEASUREMENT)

        nav.back() shouldBe true

        nav.destination.value shouldBe Destination.DIAGNOSTICS
    }

    "back on the AR screen returns to Diagnostics" {
        val nav = AppNavigationViewModel(SavedStateHandle())
        nav.open(Destination.AR_MEASUREMENT)

        nav.back() shouldBe true

        nav.destination.value shouldBe Destination.DIAGNOSTICS
    }

    "back on Diagnostics is not consumed so the system can close the app" {
        val nav = AppNavigationViewModel(SavedStateHandle())

        nav.back() shouldBe false

        nav.destination.value shouldBe Destination.DIAGNOSTICS
    }

    "back on trials returns to the screen that opened it" {
        val nav = AppNavigationViewModel(SavedStateHandle())
        nav.open(Destination.AR_MEASUREMENT)
        nav.openFieldTrials(from = Destination.AR_MEASUREMENT)
        nav.destination.value shouldBe Destination.FIELD_TRIALS

        nav.back() shouldBe true

        nav.destination.value shouldBe Destination.AR_MEASUREMENT
    }

    "the destination is written to the saved state" {
        val handle = SavedStateHandle()
        val nav = AppNavigationViewModel(handle)

        nav.openFieldTrials(from = Destination.DIAGNOSTICS)

        // A new instance on the same handle models process-death restoration.
        val restored = AppNavigationViewModel(handle)
        restored.destination.value shouldBe Destination.FIELD_TRIALS
        restored.back() shouldBe true
        restored.destination.value shouldBe Destination.DIAGNOSTICS
    }

    "after process death the AR screen is restored as Diagnostics (the ARCore session is gone)" {
        val handle = SavedStateHandle()
        AppNavigationViewModel(handle).open(Destination.AR_MEASUREMENT)

        AppNavigationViewModel(handle).destination.value shouldBe Destination.DIAGNOSTICS
    }

    "after process death trials opened from AR return to Diagnostics" {
        val handle = SavedStateHandle()
        AppNavigationViewModel(handle).openFieldTrials(from = Destination.AR_MEASUREMENT)

        val restored = AppNavigationViewModel(handle)
        restored.destination.value shouldBe Destination.FIELD_TRIALS
        restored.back() shouldBe true
        restored.destination.value shouldBe Destination.DIAGNOSTICS
    }

    "an unknown saved value falls back to Diagnostics" {
        val handle = SavedStateHandle(mapOf("destination" to "REMOVED_SCREEN"))

        AppNavigationViewModel(handle).destination.value shouldBe Destination.DIAGNOSTICS
    }
})
