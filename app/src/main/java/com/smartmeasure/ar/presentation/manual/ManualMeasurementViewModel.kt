package com.smartmeasure.ar.presentation.manual

import androidx.lifecycle.ViewModel
import com.smartmeasure.ar.domain.model.RoomGeometry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ManualMeasurementViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ManualMeasurementUiState())
    val uiState: StateFlow<ManualMeasurementUiState> = _uiState.asStateFlow()

    fun onWidthChanged(value: String) {
        _uiState.update { it.copy(widthInput = value, geometry = null, validationMessage = null) }
    }

    fun onLengthChanged(value: String) {
        _uiState.update { it.copy(lengthInput = value, geometry = null, validationMessage = null) }
    }

    fun calculateRectangle() {
        val width = _uiState.value.widthInput.toMetersOrNull()
        val length = _uiState.value.lengthInput.toMetersOrNull()

        if (width == null || length == null || width <= 0 || length <= 0) {
            _uiState.update {
                it.copy(
                    geometry = null,
                    validationMessage = "Enter width and length greater than zero.",
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                geometry = RoomGeometry.rectangle(width, length),
                validationMessage = null,
            )
        }
    }

    private fun String.toMetersOrNull(): Double? =
        trim().replace(',', '.').toDoubleOrNull()
}
