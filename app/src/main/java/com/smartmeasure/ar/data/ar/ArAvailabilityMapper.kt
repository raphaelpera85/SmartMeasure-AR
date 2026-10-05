package com.smartmeasure.ar.data.ar

import com.smartmeasure.ar.domain.model.ArAvailability

internal enum class RawArAvailability {
    SUPPORTED_INSTALLED,
    SUPPORTED_NOT_INSTALLED,
    SUPPORTED_APK_TOO_OLD,
    UNSUPPORTED,
    CHECKING,
    UNKNOWN,
}

internal fun RawArAvailability.toDomain(): ArAvailability = when (this) {
    RawArAvailability.SUPPORTED_INSTALLED -> ArAvailability.READY
    RawArAvailability.SUPPORTED_NOT_INSTALLED,
    RawArAvailability.SUPPORTED_APK_TOO_OLD,
    -> ArAvailability.NEEDS_INSTALL_OR_UPDATE

    RawArAvailability.UNSUPPORTED -> ArAvailability.UNSUPPORTED
    RawArAvailability.CHECKING -> ArAvailability.CHECKING
    RawArAvailability.UNKNOWN -> ArAvailability.UNKNOWN
}

