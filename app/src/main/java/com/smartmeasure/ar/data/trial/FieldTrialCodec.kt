package com.smartmeasure.ar.data.trial

import com.smartmeasure.ar.domain.model.ArSessionSummary
import com.smartmeasure.ar.domain.model.CaptureCondition
import com.smartmeasure.ar.domain.model.FieldTrial
import com.smartmeasure.ar.domain.model.MeasurementKind

/**
 * Versioned, line-based storage format for field trials. Pure Kotlin so it is testable on the JVM
 * (org.json is only a stub in local unit tests). Unknown or corrupt lines are skipped rather than
 * discarding the whole file.
 *
 * Format v2 (written): header `smartmeasure-field-trials<TAB>v2`, then one trial per line with 14
 * tab-separated fields — the 8 v1 fields (id, recordedAtEpochMillis, deviceModel, depthEnabled,
 * kind, arMeters, referenceMeters, conditions) followed by the AR session summary (distanceMeters,
 * trackingLosses, durationSeconds, trackingSeconds, horizontalPlanes, verticalPlanes). The six
 * session fields are all empty when the trial has no session. Doubles use Kotlin's
 * locale-independent shortest round-trip representation.
 *
 * Format v1 (read only): the same header with `v1` and the 8 trial fields; trials get no session.
 *
 * Error policy: a line with the wrong field count or an invalid trial field is skipped. A session
 * that is partially filled, not numeric or rejected by [ArSessionSummary] invariants is dropped
 * while the trial is kept, since the AR-versus-reference comparison is the valuable part.
 */
internal object FieldTrialCodec {
    private const val HEADER_PREFIX = "smartmeasure-field-trials\t"
    private const val HEADER_V1 = HEADER_PREFIX + "v1"
    private const val HEADER_V2 = HEADER_PREFIX + "v2"

    /** Always written: v1 files are migrated to v2 on the first write. */
    private const val HEADER = HEADER_V2

    private const val TRIAL_FIELD_COUNT = 8
    private const val SESSION_FIELD_COUNT = 6

    fun encode(trials: List<FieldTrial>): String = buildString {
        appendLine(HEADER)
        trials.forEach { trial ->
            appendLine((trialFields(trial) + sessionFields(trial.session)).joinToString("\t"))
        }
    }

    fun decode(text: String): List<FieldTrial> {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        val withSession = when (lines.firstOrNull()) {
            HEADER_V1 -> false
            HEADER_V2 -> true
            else -> return emptyList()
        }
        return lines.drop(1).mapNotNull { decodeLine(it, withSession) }
    }

    /**
     * True when [text] is empty or starts with a header this version reads. Anything else (a file
     * from a newer app version, or not a trial file at all) must not be overwritten, since its
     * content would be lost.
     */
    fun canOverwrite(text: String): Boolean {
        val header = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return true
        return header == HEADER_V1 || header == HEADER_V2
    }

    private fun trialFields(trial: FieldTrial): List<String> = listOf(
        trial.id,
        trial.recordedAtEpochMillis.toString(),
        trial.deviceModel,
        trial.depthEnabled.toString(),
        trial.kind.name,
        trial.arMeters.toString(),
        trial.referenceMeters.toString(),
        trial.conditions.sortedBy { it.ordinal }.joinToString(",") { it.name },
    ).map { it.sanitized() }

    private fun sessionFields(session: ArSessionSummary?): List<String> =
        if (session == null) {
            List(SESSION_FIELD_COUNT) { "" }
        } else {
            listOf(
                session.distanceMeters.toString(),
                session.trackingLosses.toString(),
                session.durationSeconds.toString(),
                session.trackingSeconds.toString(),
                session.horizontalPlanes.toString(),
                session.verticalPlanes.toString(),
            )
        }

    private fun decodeLine(line: String, withSession: Boolean): FieldTrial? {
        val fields = line.split('\t')
        val expected = if (withSession) TRIAL_FIELD_COUNT + SESSION_FIELD_COUNT else TRIAL_FIELD_COUNT
        if (fields.size != expected) return null
        val trial = runCatching {
            FieldTrial(
                id = fields[0],
                recordedAtEpochMillis = fields[1].toLong(),
                deviceModel = fields[2],
                depthEnabled = fields[3].toBooleanStrict(),
                kind = MeasurementKind.valueOf(fields[4]),
                arMeters = fields[5].toDouble(),
                referenceMeters = fields[6].toDouble(),
                conditions = fields[7]
                    .split(',')
                    .filter { it.isNotBlank() }
                    .map { CaptureCondition.valueOf(it) }
                    .toSet(),
            )
        }.getOrNull() ?: return null
        if (!withSession) return trial
        return trial.copy(session = decodeSession(fields.subList(TRIAL_FIELD_COUNT, fields.size)))
    }

    /** Null for an absent session and for any invalid one (the trial itself is still kept). */
    private fun decodeSession(fields: List<String>): ArSessionSummary? {
        if (fields.all { it.isEmpty() }) return null
        return runCatching {
            ArSessionSummary(
                distanceMeters = fields[0].toDouble(),
                trackingLosses = fields[1].toInt(),
                durationSeconds = fields[2].toDouble(),
                trackingSeconds = fields[3].toDouble(),
                horizontalPlanes = fields[4].toInt(),
                verticalPlanes = fields[5].toInt(),
            )
        }.getOrNull()
    }

    private fun String.sanitized(): String =
        replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
}
