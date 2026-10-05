package com.smartmeasure.ar.domain.model

import kotlin.math.abs

/** What was measured; errors are reported per kind because walls, openings and heights fail differently. */
enum class MeasurementKind {
    WALL,
    OPENING,
    HEIGHT,
    OTHER,
}

/** Capture conditions the Phase 0 protocol asks to rehearse. */
enum class CaptureCondition {
    LOW_LIGHT,
    PLAIN_SURFACE,
    REFLECTIVE_SURFACE,
    OBSTRUCTED,
    SMALL_ROOM,
}

/**
 * One Phase 0 accuracy trial: an AR measurement compared with the same dimension taken by a
 * reference instrument (laser or tape). Values are kept in meters; the reference is treated as
 * ground truth only for this comparison, not as a certified value.
 *
 * [session] is the summary of the AR session the measurement came from (path, tracking losses,
 * tracked time, planes), so error can be compared with capture conditions per device. It is null
 * for trials typed in manually and for trials stored before the field existed.
 */
data class FieldTrial(
    val id: String,
    val recordedAtEpochMillis: Long,
    val deviceModel: String,
    val depthEnabled: Boolean,
    val kind: MeasurementKind,
    val arMeters: Double,
    val referenceMeters: Double,
    val conditions: Set<CaptureCondition> = emptySet(),
    val session: ArSessionSummary? = null,
) {
    init {
        require(id.isNotBlank()) { "Trial id must not be blank." }
        require(arMeters.isFinite() && arMeters > 0) { "AR measurement must be greater than zero." }
        require(referenceMeters.isFinite() && referenceMeters > 0) {
            "Reference measurement must be greater than zero."
        }
    }

    /** Positive when AR overestimates the reference. */
    val signedErrorMeters: Double
        get() = arMeters - referenceMeters

    val absoluteErrorMeters: Double
        get() = abs(signedErrorMeters)

    /** Absolute error divided by the reference (0.01 = 1 %). */
    val relativeError: Double
        get() = absoluteErrorMeters / referenceMeters
}
