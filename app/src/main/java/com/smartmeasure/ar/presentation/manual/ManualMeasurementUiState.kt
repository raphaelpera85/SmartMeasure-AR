package com.smartmeasure.ar.presentation.manual

import com.smartmeasure.ar.domain.model.RoomGeometry

/** Typed input errors; the screen maps each one to a string resource (the ViewModel knows no Android resources). */
enum class ManualInputError {
    INVALID_DIMENSIONS,
}

data class ManualMeasurementUiState(
    val widthInput: String = "",
    val lengthInput: String = "",
    val geometry: RoomGeometry? = null,
    val inputError: ManualInputError? = null,
)
