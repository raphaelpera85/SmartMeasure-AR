package com.smartmeasure.ar.presentation.manual

import com.smartmeasure.ar.domain.model.RoomGeometry

data class ManualMeasurementUiState(
    val widthInput: String = "",
    val lengthInput: String = "",
    val geometry: RoomGeometry? = null,
    val validationMessage: String? = null,
)
