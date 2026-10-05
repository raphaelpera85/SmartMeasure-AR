package com.smartmeasure.ar.domain.model

enum class ArAvailability {
    CHECKING,
    READY,
    NEEDS_INSTALL_OR_UPDATE,
    UNSUPPORTED,
    UNKNOWN,
}

enum class DepthSupport {
    NOT_CHECKED,
    SUPPORTED,
    UNSUPPORTED,
}

enum class PlaneFindingSupport {
    NOT_CHECKED,
    HORIZONTAL_AND_VERTICAL,
}

enum class ArPreparationFailure {
    DEVICE_INCOMPATIBLE,
    ARCORE_NOT_INSTALLED,
    ARCORE_TOO_OLD,
    SDK_TOO_OLD,
    INSTALL_DECLINED,
    CAMERA_UNAVAILABLE,
    CONFIGURATION_UNSUPPORTED,
    UNKNOWN,
}

sealed interface ArPreparationResult {
    data object InstallRequested : ArPreparationResult

    data class Ready(
        val depthSupport: DepthSupport,
        val planeFindingSupport: PlaneFindingSupport,
    ) : ArPreparationResult

    data class Failed(
        val reason: ArPreparationFailure,
    ) : ArPreparationResult
}

