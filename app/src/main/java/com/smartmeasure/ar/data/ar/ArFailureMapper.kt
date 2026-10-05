package com.smartmeasure.ar.data.ar

import com.smartmeasure.ar.domain.model.ArPreparationFailure

/** Fixed logcat tag for AR failures; filter with `adb logcat -s SmartMeasureAR`. */
const val AR_LOG_TAG = "SmartMeasureAR"

/** ARCore/Android exception families, kept free of ARCore types so the mapping runs on the JVM. */
internal enum class RawArFailure {
    DEVICE_NOT_COMPATIBLE,
    ARCORE_NOT_INSTALLED,
    APK_TOO_OLD,
    SDK_TOO_OLD,
    USER_DECLINED_INSTALLATION,
    CAMERA_PERMISSION,
    CAMERA_NOT_AVAILABLE,
    UNSUPPORTED_CONFIGURATION,
    FATAL,
    OTHER,
}

internal fun RawArFailure.toPreparationFailure(): ArPreparationFailure = when (this) {
    RawArFailure.DEVICE_NOT_COMPATIBLE -> ArPreparationFailure.DEVICE_INCOMPATIBLE
    RawArFailure.ARCORE_NOT_INSTALLED -> ArPreparationFailure.ARCORE_NOT_INSTALLED
    RawArFailure.APK_TOO_OLD -> ArPreparationFailure.ARCORE_TOO_OLD
    RawArFailure.SDK_TOO_OLD -> ArPreparationFailure.SDK_TOO_OLD
    RawArFailure.USER_DECLINED_INSTALLATION -> ArPreparationFailure.INSTALL_DECLINED
    RawArFailure.CAMERA_PERMISSION,
    RawArFailure.CAMERA_NOT_AVAILABLE,
    -> ArPreparationFailure.CAMERA_UNAVAILABLE

    RawArFailure.UNSUPPORTED_CONFIGURATION -> ArPreparationFailure.CONFIGURATION_UNSUPPORTED
    RawArFailure.FATAL,
    RawArFailure.OTHER,
    -> ArPreparationFailure.UNKNOWN
}

/** Log text: failing stage plus exception class and message only (no personal data). */
fun arFailureLogMessage(
    stage: String,
    exceptionClass: String,
    exceptionMessage: String?,
): String = buildString {
    append("AR ").append(stage).append(" failed: ").append(exceptionClass)
    if (exceptionMessage != null) append(": ").append(exceptionMessage)
}
