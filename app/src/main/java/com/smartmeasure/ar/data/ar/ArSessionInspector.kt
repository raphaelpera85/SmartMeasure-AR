package com.smartmeasure.ar.data.ar

import android.app.Activity
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.FatalException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.core.exceptions.UnsupportedConfigurationException
import com.smartmeasure.ar.domain.model.ArPreparationFailure
import com.smartmeasure.ar.domain.model.ArPreparationResult
import com.smartmeasure.ar.domain.model.DepthSupport
import com.smartmeasure.ar.domain.model.PlaneFindingSupport

class ArSessionInspector {
    fun prepare(
        activity: Activity,
        userRequestedInstall: Boolean,
    ): ArPreparationResult = try {
        when (ArCoreApk.getInstance().requestInstall(activity, userRequestedInstall)) {
            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> ArPreparationResult.InstallRequested
            ArCoreApk.InstallStatus.INSTALLED -> inspectSession(activity)
        }
    } catch (e: Exception) {
        failed(stage = "install check", error = e)
    }

    private fun inspectSession(activity: Activity): ArPreparationResult {
        val session = try {
            Session(activity)
        } catch (e: Exception) {
            return failed(stage = "session creation", error = e)
        }

        return try {
            val config = session.config.apply {
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            }

            val depthSupport = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
                DepthSupport.SUPPORTED
            } else {
                DepthSupport.UNSUPPORTED
            }

            session.configure(config)

            ArPreparationResult.Ready(
                depthSupport = depthSupport,
                planeFindingSupport = PlaneFindingSupport.HORIZONTAL_AND_VERTICAL,
            )
        } catch (e: Exception) {
            failed(stage = "session configuration", error = e)
        } finally {
            session.close()
        }
    }

    private fun failed(stage: String, error: Exception): ArPreparationResult.Failed {
        val reason: ArPreparationFailure = error.toRawArFailure().toPreparationFailure()
        Log.w(
            AR_LOG_TAG,
            arFailureLogMessage(stage, error.javaClass.name, error.message) + " -> $reason",
            error,
        )
        return ArPreparationResult.Failed(reason)
    }
}

/** Classifies ARCore/Android exceptions; the testable mapping lives in [toPreparationFailure]. */
internal fun Exception.toRawArFailure(): RawArFailure = when (this) {
    is UnavailableDeviceNotCompatibleException -> RawArFailure.DEVICE_NOT_COMPATIBLE
    is UnavailableArcoreNotInstalledException -> RawArFailure.ARCORE_NOT_INSTALLED
    is UnavailableApkTooOldException -> RawArFailure.APK_TOO_OLD
    is UnavailableSdkTooOldException -> RawArFailure.SDK_TOO_OLD
    is UnavailableUserDeclinedInstallationException -> RawArFailure.USER_DECLINED_INSTALLATION
    is SecurityException -> RawArFailure.CAMERA_PERMISSION
    is CameraNotAvailableException -> RawArFailure.CAMERA_NOT_AVAILABLE
    is UnsupportedConfigurationException -> RawArFailure.UNSUPPORTED_CONFIGURATION
    is FatalException -> RawArFailure.FATAL
    else -> RawArFailure.OTHER
}
