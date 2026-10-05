package com.smartmeasure.ar.data.ar

import com.smartmeasure.ar.domain.model.ArAvailability
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ArAvailabilityMapperTest : StringSpec({
    "installed ARCore is ready" {
        RawArAvailability.SUPPORTED_INSTALLED.toDomain() shouldBe ArAvailability.READY
    }

    "missing or old ARCore asks for setup" {
        RawArAvailability.SUPPORTED_NOT_INSTALLED.toDomain() shouldBe
            ArAvailability.NEEDS_INSTALL_OR_UPDATE
        RawArAvailability.SUPPORTED_APK_TOO_OLD.toDomain() shouldBe
            ArAvailability.NEEDS_INSTALL_OR_UPDATE
    }

    "unsupported device stays unsupported" {
        RawArAvailability.UNSUPPORTED.toDomain() shouldBe ArAvailability.UNSUPPORTED
    }

    "transient availability stays checking" {
        RawArAvailability.CHECKING.toDomain() shouldBe ArAvailability.CHECKING
    }

    "unknown availability remains explicit" {
        RawArAvailability.UNKNOWN.toDomain() shouldBe ArAvailability.UNKNOWN
    }
})

