package com.smartmeasure.ar.data.ar

import android.content.Context
import com.google.ar.core.ArCoreApk
import com.smartmeasure.ar.domain.model.ArAvailability
import com.smartmeasure.ar.domain.repository.ArAvailabilityRepository

class ArCoreAvailabilityRepository(
    context: Context,
) : ArAvailabilityRepository {
    private val applicationContext = context.applicationContext

    override fun currentAvailability(): ArAvailability =
        ArCoreApk.getInstance()
            .checkAvailability(applicationContext)
            .toRaw()
            .toDomain()

    private fun ArCoreApk.Availability.toRaw(): RawArAvailability = when (this) {
        ArCoreApk.Availability.SUPPORTED_INSTALLED -> RawArAvailability.SUPPORTED_INSTALLED
        ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> RawArAvailability.SUPPORTED_NOT_INSTALLED
        ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> RawArAvailability.SUPPORTED_APK_TOO_OLD
        ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> RawArAvailability.UNSUPPORTED
        ArCoreApk.Availability.UNKNOWN_CHECKING -> RawArAvailability.CHECKING
        ArCoreApk.Availability.UNKNOWN_ERROR,
        ArCoreApk.Availability.UNKNOWN_TIMED_OUT,
        -> RawArAvailability.UNKNOWN
    }
}

