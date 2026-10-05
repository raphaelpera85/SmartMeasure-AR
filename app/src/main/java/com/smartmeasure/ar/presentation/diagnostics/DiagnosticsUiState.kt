package com.smartmeasure.ar.presentation.diagnostics

import com.smartmeasure.ar.domain.model.ArAvailability
import com.smartmeasure.ar.domain.model.ArPreparationFailure
import com.smartmeasure.ar.domain.model.DepthSupport
import com.smartmeasure.ar.domain.model.PlaneFindingSupport

data class DiagnosticsUiState(
    val arAvailability: ArAvailability = ArAvailability.CHECKING,
    val depthSupport: DepthSupport = DepthSupport.NOT_CHECKED,
    val planeFindingSupport: PlaneFindingSupport = PlaneFindingSupport.NOT_CHECKED,
    val preparationState: PreparationState = PreparationState.IDLE,
    val failure: ArPreparationFailure? = null,
) {
    val canPrepareAr: Boolean
        get() = arAvailability == ArAvailability.READY ||
            arAvailability == ArAvailability.NEEDS_INSTALL_OR_UPDATE
}

enum class PreparationState {
    IDLE,
    PREPARING,
    INSTALL_REQUESTED,
    READY,
    PERMISSION_DENIED,
    FAILED,
}

