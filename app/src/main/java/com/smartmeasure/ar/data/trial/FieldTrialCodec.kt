package com.smartmeasure.ar.data.trial

import com.smartmeasure.ar.domain.model.CaptureCondition
import com.smartmeasure.ar.domain.model.FieldTrial
import com.smartmeasure.ar.domain.model.MeasurementKind

/**
 * Versioned, line-based storage format for field trials. Pure Kotlin so it is testable on the JVM
 * (org.json is only a stub in local unit tests). Unknown or corrupt lines are skipped rather than
 * discarding the whole file.
 */
internal object FieldTrialCodec {
    const val HEADER = "smartmeasure-field-trials\tv1"
    private const val FIELD_COUNT = 8

    fun encode(trials: List<FieldTrial>): String = buildString {
        appendLine(HEADER)
        trials.forEach { trial ->
            appendLine(
                listOf(
                    trial.id,
                    trial.recordedAtEpochMillis.toString(),
                    trial.deviceModel,
                    trial.depthEnabled.toString(),
                    trial.kind.name,
                    trial.arMeters.toString(),
                    trial.referenceMeters.toString(),
                    trial.conditions.sortedBy { it.ordinal }.joinToString(",") { it.name },
                ).joinToString("\t") { it.sanitized() },
            )
        }
    }

    fun decode(text: String): List<FieldTrial> {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty() || lines.first() != HEADER) return emptyList()
        return lines.drop(1).mapNotNull(::decodeLine)
    }

    private fun decodeLine(line: String): FieldTrial? {
        val fields = line.split('\t')
        if (fields.size != FIELD_COUNT) return null
        return runCatching {
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
        }.getOrNull()
    }

    private fun String.sanitized(): String =
        replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
}
