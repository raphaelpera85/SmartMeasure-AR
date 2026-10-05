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

    "a new AR session clears the previous measurement whose anchors no longer exist" {
        val viewModel = ArMeasurementViewModel()
        viewModel.onSessionReady(depthEnabled = false)
        viewModel.onTrackingChanged(tracking = true)
        viewModel.onPointCaptured(pointCount = 1, distanceMeters = null)
        viewModel.onPointCaptured(pointCount = 2, distanceMeters = 2.4)

        // Rotation recreates ArMeasureView: a new session without anchors.
        viewModel.onSessionReady(depthEnabled = true)

        val state = viewModel.uiState.value
        state.capturedPoints shouldBe 0
        state.distanceMeters shouldBe null
        state.tracking shouldBe false
        state.sessionReady shouldBe true
        state.depthEnabled shouldBe true
        state.message shouldBe ArMeasurementMessage.MOVE_PHONE
    }

    "tracking after a new session asks for the first point again" {
        val viewModel = ArMeasurementViewModel()
        viewModel.onSessionReady(depthEnabled = false)
        viewModel.onTrackingChanged(tracking = true)
        viewModel.onPointCaptured(pointCount = 1, distanceMeters = null)

        viewModel.onSessionReady(depthEnabled = false)
        viewModel.onTrackingChanged(tracking = true)

        viewModel.uiState.value.message shouldBe ArMeasurementMessage.AIM_AND_CAPTURE_FIRST
    }

    "resetting the measurement keeps the session summary" {
        val viewModel = ArMeasurementViewModel()
        viewModel.onSessionSummary(SAMPLE_SUMMARY)

        viewModel.onReset()

        viewModel.uiState.value.sessionSummary shouldBe SAMPLE_SUMMARY
    }
})
