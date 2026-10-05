package com.smartmeasure.ar.domain.model

import kotlin.math.ceil

/**
 * Error summary for one group of comparable trials. Groups never mix measurement kinds or
 * depth/no-depth sessions, so different capture origins are not averaged as if equally precise.
 */
data class TrialSummary(
    val kind: MeasurementKind,
    val depthEnabled: Boolean,
    val count: Int,
    val meanAbsoluteErrorMeters: Double,
    val medianAbsoluteErrorMeters: Double,
    /** Nearest-rank 90th percentile; with few samples it equals the maximum. */
    val p90AbsoluteErrorMeters: Double,
    val maxAbsoluteErrorMeters: Double,
    /** Mean signed error; a value far from zero indicates systematic over/underestimation. */
    val meanSignedErrorMeters: Double,
    val meanRelativeError: Double,
) {
    /**
     * True when the group has at least [TrialStatistics.MIN_REPRESENTATIVE_SAMPLES] trials.
     * Below that, the nearest-rank P90 coincides with the maximum (rank = ceil(0.9 · n) = n for
     * n ≤ 9), so it is not a stable percentile and the summary must be shown as a small,
     * non-representative sample.
     */
    val isRepresentative: Boolean
        get() = count >= TrialStatistics.MIN_REPRESENTATIVE_SAMPLES
}

object TrialStatistics {
    /**
     * Minimum trials per group for the summary to be considered representative. With fewer
     * samples the nearest-rank P90 equals the maximum error and is not a stable estimate.
     */
    const val MIN_REPRESENTATIVE_SAMPLES: Int = 10

    fun summarize(trials: List<FieldTrial>): List<TrialSummary> =
        trials
            .groupBy { it.kind to it.depthEnabled }
            .map { (key, group) -> summarizeGroup(key.first, key.second, group) }
            .sortedWith(compareBy<TrialSummary> { it.kind.ordinal }.thenBy { it.depthEnabled })

    private fun summarizeGroup(
        kind: MeasurementKind,
        depthEnabled: Boolean,
        group: List<FieldTrial>,
    ): TrialSummary {
        val absoluteErrors = group.map { it.absoluteErrorMeters }.sorted()
        return TrialSummary(
            kind = kind,
            depthEnabled = depthEnabled,
            count = group.size,
            meanAbsoluteErrorMeters = absoluteErrors.average(),
            medianAbsoluteErrorMeters = median(absoluteErrors),
            p90AbsoluteErrorMeters = nearestRankPercentile(absoluteErrors, 0.90),
            maxAbsoluteErrorMeters = absoluteErrors.last(),
            meanSignedErrorMeters = group.map { it.signedErrorMeters }.average(),
            meanRelativeError = group.map { it.relativeError }.average(),
        )
    }

    internal fun median(sorted: List<Double>): Double {
        require(sorted.isNotEmpty())
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }

    internal fun nearestRankPercentile(sorted: List<Double>, fraction: Double): Double {
        require(sorted.isNotEmpty())
        require(fraction in 0.0..1.0)
        val rank = ceil(fraction * sorted.size).toInt().coerceIn(1, sorted.size)
        return sorted[rank - 1]
    }
}
