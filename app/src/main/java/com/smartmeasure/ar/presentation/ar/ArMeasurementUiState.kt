package com.smartmeasure.ar.presentation.ar

import com.smartmeasure.ar.domain.model.ArSessionSummary

data class ArMeasurementUiState(
    val sessionReady: Boolean = false,
    val tracking: Boolean = false,
    val depthEnabled: Boolean = false,
    val capturedPoints: Int = 0,
    val distanceMeters: Double? = null,
    val message: ArMeasurementMessage = ArMeasurementMessage.STARTING,
    /** Path and tracking quality of the current AR session; null until the first update. */
    val sessionSummary: ArSessionSummary? = null,
)

enum class ArMeasurementMessage {
    STARTING,
    MOVE_PHONE,
    AIM_AND_CAPTURE_FIRST,
    AIM_AND_CAPTURE_SECOND,
    MEASUREMENT_READY,
    NO_SURFACE,
    SESSION_ERROR,
}
