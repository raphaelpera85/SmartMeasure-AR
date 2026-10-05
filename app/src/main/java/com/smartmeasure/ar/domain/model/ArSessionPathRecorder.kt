package com.smartmeasure.ar.domain.model

import kotlin.math.sqrt

/**
 * Path and tracking quality of one AR measurement session, used to compare devices and
 * capture conditions. Values are observations of the session, not an accuracy figure.
 *
 * @property distanceMeters camera path length while tracking (see [ArSessionPathRecorder]).
 * @property trackingLosses transitions from tracking to not tracking after the first fix.
 * @property durationSeconds session time with frames, excluding gaps over the limit.
 * @property trackingSeconds part of [durationSeconds] spent tracking.
 * @property horizontalPlanes tracked horizontal planes (floor, tables, ceiling).
 * @property verticalPlanes tracked vertical planes (walls).
 *
 * Invariants (checked in init, so any producer — recorder, storage, manual entry — gets them):
 * every value is finite and non-negative, and [trackingSeconds] does not exceed
 * [durationSeconds] by more than [TIME_TOLERANCE_SECONDS].
 */
data class ArSessionSummary(
    val distanceMeters: Double,
    val trackingLosses: Int,
    val durationSeconds: Double,
    val trackingSeconds: Double,
    val horizontalPlanes: Int,
    val verticalPlanes: Int,
) {
    init {
        require(distanceMeters.isFinite() && distanceMeters >= 0) { "Distance must be finite and >= 0." }
        require(trackingLosses >= 0) { "Tracking losses must be >= 0." }
        require(durationSeconds.isFinite() && durationSeconds >= 0) { "Duration must be finite and >= 0." }
        require(trackingSeconds.isFinite() && trackingSeconds >= 0) {
            "Tracking time must be finite and >= 0."
        }
        require(trackingSeconds <= durationSeconds + TIME_TOLERANCE_SECONDS) {
            "Tracking time must not exceed the session duration."
        }
        require(horizontalPlanes >= 0 && verticalPlanes >= 0) { "Plane counts must be >= 0." }
    }

    /**
     * Share of [durationSeconds] spent tracking, in 0..1 (capped at 1 when rounding puts tracking
     * time within [TIME_TOLERANCE_SECONDS] above the duration); null when the session has no
     * duration, since the share is undefined.
     */
    val trackingRatio: Double?
        get() = if (durationSeconds > 0) (trackingSeconds / durationSeconds).coerceAtMost(1.0) else null

    companion object {
        /**
         * Allowance (1 µs) for tracking time above duration caused by rounding when the summary is
         * rebuilt from stored or exported values. The recorder itself never exceeds the duration.
         */
        const val TIME_TOLERANCE_SECONDS = 1e-6
    }
}

/**
 * Records the camera path of one AR session from per-frame samples.
 *
 * Distance uses a reference point: a new position only adds distance once it is at least
 * [MIN_STEP_METERS] away (within [STEP_TOLERANCE_METERS] of float rounding) from the last
 * counted position, which then moves to it. Pose jitter
 * smaller than that never accumulates, independent of the frame rate. Slow, curved paths are
 * approximated by chords of at least [MIN_STEP_METERS], so the total is a slight underestimate.
 *
 * When tracking is lost the reference point is dropped, so the jump between the last pose before
 * the loss and the first pose after relocalization is not summed.
 *
 * Time: each interval between consecutive samples takes the tracking state of the sample that
 * starts it. Intervals longer than [MAX_SAMPLE_GAP_NANOS] (session paused, app in background)
 * count neither as time nor as distance. Non-increasing timestamps add no time. The owner calls
 * [markInterrupted] when the session pauses, so shorter pauses are excluded as well.
 *
 * Takes primitives so the GL thread does not allocate a sample object per frame.
 * Not thread-safe: feed and read it from a single thread (the GL thread).
 */
class ArSessionPathRecorder {
    private var hasReference = false
    private var refX = 0f
    private var refY = 0f
    private var refZ = 0f
    private var distanceMeters = 0.0
    private var trackingLosses = 0
    private var lastTracking = false
    private var hasSample = false
    private var lastTimestampNanos = 0L
    private var durationNanos = 0L
    private var trackingNanos = 0L

    fun addSample(timestampNanos: Long, tracking: Boolean, x: Float, y: Float, z: Float) {
        if (hasSample) {
            val dt = timestampNanos - lastTimestampNanos
            if (dt > MAX_SAMPLE_GAP_NANOS) {
                hasReference = false
            } else if (dt > 0) {
                durationNanos += dt
                if (lastTracking) trackingNanos += dt
            }
        }
        if (!hasSample || timestampNanos > lastTimestampNanos) lastTimestampNanos = timestampNanos
        hasSample = true

        if (!tracking) {
            if (lastTracking) trackingLosses++
            lastTracking = false
            hasReference = false
            return
        }
        lastTracking = true
        if (!hasReference) {
            setReference(x, y, z)
            return
        }
        val dx = (x - refX).toDouble()
        val dy = (y - refY).toDouble()
        val dz = (z - refZ).toDouble()
        val step = sqrt(dx * dx + dy * dy + dz * dz)
        if (step >= MIN_STEP_METERS - STEP_TOLERANCE_METERS) {
            distanceMeters += step
            setReference(x, y, z)
        }
    }

    /**
     * The session was paused (or otherwise stopped delivering frames): the interval up to the
     * next sample counts neither as time nor as distance, even if shorter than
     * [MAX_SAMPLE_GAP_NANOS]. Tracking state and loss count are kept, as for a long gap.
     */
    fun markInterrupted() {
        // Same effect as a gap over the limit: the next sample only sets the timestamp and the
        // reference point. lastTracking stays, so not tracking after resume still counts a loss.
        hasSample = false
        hasReference = false
    }

    /** Plane counts come from the caller (ARCore trackables), not from the samples. */
    fun summary(horizontalPlanes: Int = 0, verticalPlanes: Int = 0): ArSessionSummary =
        ArSessionSummary(
            distanceMeters = distanceMeters,
            trackingLosses = trackingLosses,
            durationSeconds = durationNanos / NANOS_PER_SECOND,
            trackingSeconds = trackingNanos / NANOS_PER_SECOND,
            horizontalPlanes = horizontalPlanes,
            verticalPlanes = verticalPlanes,
        )

    private fun setReference(x: Float, y: Float, z: Float) {
        refX = x
        refY = y
        refZ = z
        hasReference = true
    }

    companion object {
        /** Minimum displacement (2 cm) counted as movement; smaller pose changes are jitter. */
        const val MIN_STEP_METERS = 0.02

        /**
         * Float rounding allowance (10 µm) for the threshold comparison. Poses arrive as Float:
         * 0.02f widens to 0.0199999995, and a coordinate difference near 100 m carries ~8 µm of
         * rounding, so a step of exactly [MIN_STEP_METERS] would otherwise be dropped. Physically
         * negligible next to the 2 cm threshold.
         */
        const val STEP_TOLERANCE_METERS = 1e-5

        /** Longest interval between samples (1 s) still counted; longer means a paused session. */
        const val MAX_SAMPLE_GAP_NANOS = 1_000_000_000L

        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
