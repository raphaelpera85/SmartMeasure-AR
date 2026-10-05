package com.smartmeasure.ar.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private fun summary(
    distance: Double = 3.5,
    losses: Int = 1,
    duration: Double = 20.0,
    tracking: Double = 18.0,
    horizontal: Int = 1,
    vertical: Int = 2,
) = ArSessionSummary(
    distanceMeters = distance,
    trackingLosses = losses,
    durationSeconds = duration,
    trackingSeconds = tracking,
    horizontalPlanes = horizontal,
    verticalPlanes = vertical,
)

class ArSessionSummaryTest : StringSpec({
    "tracking ratio is tracking time over duration" {
        summary(duration = 20.0, tracking = 18.0).trackingRatio!! shouldBe (0.9 plusOrMinus 1e-12)
        summary(duration = 3.0, tracking = 2.0).trackingRatio!! shouldBe (2.0 / 3.0 plusOrMinus 1e-12)
    }

    "tracking ratio is undefined for a session without duration" {
        summary(duration = 0.0, tracking = 0.0).trackingRatio shouldBe null
    }

    "an empty session (all zeros, as an unfed recorder reports) is valid" {
        val empty = ArSessionPathRecorder().summary()
        empty shouldBe summary(0.0, 0, 0.0, 0.0, 0, 0)
    }

    "tracking the whole session is the upper edge" {
        summary(duration = 5.0, tracking = 5.0).trackingRatio shouldBe 1.0
    }

    "tracking above duration within the float tolerance is accepted and capped at 1" {
        val tolerance = ArSessionSummary.TIME_TOLERANCE_SECONDS
        val s = summary(duration = 5.0, tracking = 5.0 + tolerance / 2)
        s.trackingRatio shouldBe 1.0
    }

    "tracking above duration beyond the tolerance is rejected" {
        val tolerance = ArSessionSummary.TIME_TOLERANCE_SECONDS
        shouldThrow<IllegalArgumentException> { summary(duration = 5.0, tracking = 5.0 + tolerance * 2) }
        shouldThrow<IllegalArgumentException> { summary(duration = 0.0, tracking = 1.0) }
    }

    "negative values are rejected" {
        shouldThrow<IllegalArgumentException> { summary(distance = -0.01) }
        shouldThrow<IllegalArgumentException> { summary(losses = -1) }
        shouldThrow<IllegalArgumentException> { summary(duration = -1.0, tracking = 0.0) }
        shouldThrow<IllegalArgumentException> { summary(tracking = -1.0) }
        shouldThrow<IllegalArgumentException> { summary(horizontal = -1) }
        shouldThrow<IllegalArgumentException> { summary(vertical = -1) }
    }

    "non-finite values are rejected" {
        shouldThrow<IllegalArgumentException> { summary(distance = Double.NaN) }
        shouldThrow<IllegalArgumentException> { summary(distance = Double.POSITIVE_INFINITY) }
        shouldThrow<IllegalArgumentException> { summary(duration = Double.POSITIVE_INFINITY) }
        shouldThrow<IllegalArgumentException> { summary(tracking = Double.NaN) }
    }
})
