package com.smartmeasure.ar.presentation.ar

import androidx.lifecycle.ViewModel
import com.smartmeasure.ar.domain.model.ArSessionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ArMeasurementViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ArMeasurementUiState())
    val uiState: StateFlow<ArMeasurementUiState> = _uiState.asStateFlow()

    fun onSessionReady(depthEnabled: Boolean) {
        _uiState.update {
            it.copy(
                sessionReady = true,
                depthEnabled = depthEnabled,
                sessionSummary = null,
                message = ArMeasurementMessage.MOVE_PHONE,
            )
        }
    }

    fun onTrackingChanged(tracking: Boolean) {
        _uiState.update { current ->
            current.copy(
                tracking = tracking,
                message = when {
                    !tracking -> ArMeasurementMessage.MOVE_PHONE
                    current.distanceMeters != null -> ArMeasurementMessage.MEASUREMENT_READY
                    current.capturedPoints == 1 -> ArMeasurementMessage.AIM_AND_CAPTURE_SECOND
                    else -> ArMeasurementMessage.AIM_AND_CAPTURE_FIRST
                },
            )
        }
    }

    fun onPointCaptured(pointCount: Int, distanceMeters: Double?) {
        _uiState.update {
            it.copy(
                capturedPoints = pointCount,
                distanceMeters = distanceMeters,
                message = when {
                    distanceMeters != null -> ArMeasurementMessage.MEASUREMENT_READY
                    pointCount == 1 -> ArMeasurementMessage.AIM_AND_CAPTURE_SECOND
                    else -> ArMeasurementMessage.AIM_AND_CAPTURE_FIRST
                },
            )
        }
    }

    fun onSessionSummary(summary: ArSessionSummary) {
        _uiState.update { it.copy(sessionSummary = summary) }
    }

    fun onNoSurface() {
        _uiState.update { it.copy(message = ArMeasurementMessage.NO_SURFACE) }
    }

    fun onSessionError() {
        _uiState.update {
            it.copy(
                sessionReady = false,
                tracking = false,
                message = ArMeasurementMessage.SESSION_ERROR,
            )
        }
    }

    fun onReset() {
        _uiState.update {
            it.copy(
                capturedPoints = 0,
                distanceMeters = null,
                message = if (it.tracking) {
                    ArMeasurementMessage.AIM_AND_CAPTURE_FIRST
                } else {
                    ArMeasurementMessage.MOVE_PHONE
                },
            )
        }
    }
}
