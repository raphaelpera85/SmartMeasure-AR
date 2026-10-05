package com.smartmeasure.ar.data.ar

import android.app.Activity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
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
    } catch (_: UnavailableDeviceNotCompatibleException) {
        failed(ArPreparationFailure.DEVICE_INCOMPATIBLE)
    } catch (_: UnavailableArcoreNotInstalledException) {
        failed(ArPreparationFailure.ARCORE_NOT_INSTALLED)
    } catch (_: UnavailableApkTooOldException) {
        failed(ArPreparationFailure.ARCORE_TOO_OLD)
    } catch (_: UnavailableSdkTooOldException) {
        failed(ArPreparationFailure.SDK_TOO_OLD)
    } catch (_: UnavailableUserDeclinedInstallationException) {
        failed(ArPreparationFailure.INSTALL_DECLINED)
    } catch (_: SecurityException) {
        failed(ArPreparationFailure.CAMERA_UNAVAILABLE)
    } catch (_: Exception) {
        failed(ArPreparationFailure.UNKNOWN)
    }

    private fun inspectSession(activity: Activity): ArPreparationResult {
        val session = try {
            Session(activity)
        } catch (_: UnavailableDeviceNotCompatibleException) {
            return failed(ArPreparationFailure.DEVICE_INCOMPATIBLE)
        } catch (_: UnavailableArcoreNotInstalledException) {
            return failed(ArPreparationFailure.ARCORE_NOT_INSTALLED)
        } catch (_: UnavailableApkTooOldException) {
            return failed(ArPreparationFailure.ARCORE_TOO_OLD)
        } catch (_: UnavailableSdkTooOldException) {
            return failed(ArPreparationFailure.SDK_TOO_OLD)
        } catch (_: SecurityException) {
            return failed(ArPreparationFailure.CAMERA_UNAVAILABLE)
        } catch (_: Exception) {
            return failed(ArPreparationFailure.UNKNOWN)
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
        } catch (_: UnsupportedConfigurationException) {
            failed(ArPreparationFailure.CONFIGURATION_UNSUPPORTED)
        } catch (_: CameraNotAvailableException) {
            failed(ArPreparationFailure.CAMERA_UNAVAILABLE)
        } catch (_: Exception) {
            failed(ArPreparationFailure.UNKNOWN)
        } finally {
            session.close()
        }
    }

    private fun failed(reason: ArPreparationFailure) =
        ArPreparationResult.Failed(reason)
}

