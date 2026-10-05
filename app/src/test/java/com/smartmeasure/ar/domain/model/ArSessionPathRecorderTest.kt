package com.smartmeasure.ar.domain.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val FRAME_NANOS = 33_000_000L // ~30 fps

private fun ArSessionPathRecorder.track(frame: Int, x: Float, y: Float, z: Float) =
    addSample(frame * FRAME_NANOS, tracking = true, x = x, y = y, z = z)

private fun ArSessionPathRecorder.lost(frame: Int) =
    addSample(frame * FRAME_NANOS, tracking = false, x = 0f, y = 0f, z = 0f)

class ArSessionPathRecorderTest : StringSpec({
    "jitter below the step threshold does not add distance" {
        val recorder = ArSessionPathRecorder()
        val offsets = floatArrayOf(0f, 0.004f, -0.003f, 0.005f, -0.004f, 0.002f)

        repeat(60) { frame ->
            val d = offsets[frame % offsets.size]
            recorder.track(frame + 1, 1f + d, 1.4f - d, 2f + d)
        }

        recorder.summary().distanceMeters shouldBe (0.0 plusOrMinus 0.001)
    }

    "L-shaped walk sums both legs" {
        val recorder = ArSessionPathRecorder()
        var frame = 1
        // 2 m along x in 5 cm steps, then 1.5 m along z.
        for (i in 0..40) recorder.track(frame++, i * 0.05f, 1.4f, 0f)
        for (i in 1..30) recorder.track(frame++, 2f, 1.4f, i * 0.05f)

        recorder.summary().distanceMeters shouldBe (3.5 plusOrMinus 0.01)
    }

    "jump after tracking loss is not summed and counts one loss" {
        val recorder = ArSessionPathRecorder()
        recorder.track(1, 0f, 0f, 0f)
        recorder.track(2, 0.5f, 0f, 0f)
        recorder.lost(3)
        recorder.lost(4)
        // Relocalized 3 m away: the jump must be ignored.
        recorder.track(5, 3.5f, 0f, 0f)
        recorder.track(6, 4.0f, 0f, 0f)

        val summary = recorder.summary()
        summary.distanceMeters shouldBe (1.0 plusOrMinus 0.001)
        summary.trackingLosses shouldBe 1
    }

    "not tracking before the first fix is not a loss" {
        val recorder = ArSessionPathRecorder()
        recorder.lost(1)
        recorder.lost(2)
        recorder.track(3, 0f, 0f, 0f)

        recorder.summary().trackingLosses shouldBe 0
    }

    "tracked time excludes the time spent lost" {
        val recorder = ArSessionPathRecorder()
        val ms = 1_000_000L
        // Tracking 0..1000 ms, lost 1100..1500 ms, tracking again 1600..2000 ms (100 ms frames).
        for (t in 0..20) {
            val tracking = t <= 10 || t >= 16
            recorder.addSample(t * 100 * ms, tracking, x = 0f, y = 0f, z = 0f)
        }

        val summary = recorder.summary()
        summary.durationSeconds shouldBe (2.0 plusOrMinus 1e-9)
        // Each interval takes the state of the sample that starts it: 0..1100 and 1600..2000 ms.
        summary.trackingSeconds shouldBe (1.5 plusOrMinus 1e-9)
    }

    "gap longer than the limit (paused session) adds neither time nor distance" {
        val recorder = ArSessionPathRecorder()
        val ms = 1_000_000L
        recorder.addSample(0, tracking = true, x = 0f, y = 0f, z = 0f)
        recorder.addSample(100 * ms, tracking = true, x = 0.1f, y = 0f, z = 0f)
        recorder.addSample(5_100 * ms, tracking = true, x = 3.0f, y = 0f, z = 0f)
        recorder.addSample(5_200 * ms, tracking = true, x = 3.1f, y = 0f, z = 0f)

        val summary = recorder.summary()
        summary.durationSeconds shouldBe (0.2 plusOrMinus 1e-9)
        summary.trackingSeconds shouldBe (0.2 plusOrMinus 1e-9)
        summary.distanceMeters shouldBe (0.2 plusOrMinus 0.001)
        summary.trackingLosses shouldBe 0
    }

    "summary carries the tracked plane counts" {
        val summary = ArSessionPathRecorder().summary(horizontalPlanes = 2, verticalPlanes = 3)

        summary.horizontalPlanes shouldBe 2
        summary.verticalPlanes shouldBe 3
        summary.durationSeconds shouldBe 0.0
    }
})
