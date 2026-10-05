package com.smartmeasure.ar.presentation.ar

import com.smartmeasure.ar.domain.model.ArSessionSummary
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val SAMPLE_SUMMARY = ArSessionSummary(
    distanceMeters = 3.5,
    trackingLosses = 1,
    durationSeconds = 20.0,
    trackingSeconds = 18.0,
    horizontalPlanes = 1,
    verticalPlanes = 2,
)

class ArMeasurementViewModelTest : StringSpec({
    "session summary updates are exposed in the UI state" {
        val viewModel = ArMeasurementViewModel()

        viewModel.onSessionSummary(SAMPLE_SUMMARY)

        viewModel.uiState.value.sessionSummary shouldBe SAMPLE_SUMMARY
    }

    "a new AR session clears the previous session summary" {
        val viewModel = ArMeasurementViewModel()
        viewModel.onSessionSummary(SAMPLE_SUMMARY)

        viewModel.onSessionReady(depthEnabled = false)

        viewModel.uiState.value.sessionSummary shouldBe null
    }

    "resetting the measurement keeps the session summary" {
        val viewModel = ArMeasurementViewModel()
        viewModel.onSessionSummary(SAMPLE_SUMMARY)

        viewModel.onReset()

        viewModel.uiState.value.sessionSummary shouldBe SAMPLE_SUMMARY
    }
})
