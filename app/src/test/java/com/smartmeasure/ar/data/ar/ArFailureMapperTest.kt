package com.smartmeasure.ar.data.ar

import com.smartmeasure.ar.domain.model.ArPreparationFailure
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ArFailureMapperTest : StringSpec({
    "ARCore availability exceptions map to their specific reasons" {
        RawArFailure.DEVICE_NOT_COMPATIBLE.toPreparationFailure() shouldBe
            ArPreparationFailure.DEVICE_INCOMPATIBLE
        RawArFailure.ARCORE_NOT_INSTALLED.toPreparationFailure() shouldBe
            ArPreparationFailure.ARCORE_NOT_INSTALLED
        RawArFailure.APK_TOO_OLD.toPreparationFailure() shouldBe
            ArPreparationFailure.ARCORE_TOO_OLD
        RawArFailure.SDK_TOO_OLD.toPreparationFailure() shouldBe
            ArPreparationFailure.SDK_TOO_OLD
        RawArFailure.USER_DECLINED_INSTALLATION.toPreparationFailure() shouldBe
            ArPreparationFailure.INSTALL_DECLINED
    }

    "camera permission and camera availability failures mean the camera is unavailable" {
        RawArFailure.CAMERA_PERMISSION.toPreparationFailure() shouldBe
            ArPreparationFailure.CAMERA_UNAVAILABLE
        RawArFailure.CAMERA_NOT_AVAILABLE.toPreparationFailure() shouldBe
            ArPreparationFailure.CAMERA_UNAVAILABLE
    }

    "an unsupported session configuration is reported as such" {
        RawArFailure.UNSUPPORTED_CONFIGURATION.toPreparationFailure() shouldBe
            ArPreparationFailure.CONFIGURATION_UNSUPPORTED
    }

    "ARCore fatal errors and unexpected exceptions stay unknown" {
        RawArFailure.FATAL.toPreparationFailure() shouldBe ArPreparationFailure.UNKNOWN
        RawArFailure.OTHER.toPreparationFailure() shouldBe ArPreparationFailure.UNKNOWN
    }

    "the log line names the exception class and message without anything else" {
        arFailureLogMessage(
            stage = "session",
            exceptionClass = "com.google.ar.core.exceptions.FatalException",
            exceptionMessage = "Camera failure",
        ) shouldBe "AR session failed: com.google.ar.core.exceptions.FatalException: Camera failure"
    }

    "the log line tolerates exceptions without a message" {
        arFailureLogMessage(
            stage = "prepare",
            exceptionClass = "java.lang.IllegalStateException",
            exceptionMessage = null,
        ) shouldBe "AR prepare failed: java.lang.IllegalStateException"
    }
})
