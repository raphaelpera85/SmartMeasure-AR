package com.smartmeasure.ar.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Spreadsheet-friendly export. Always uses '.' decimals and UTC timestamps regardless of locale. */
object FieldTrialCsv {
    private val header = listOf(
        "id",
        "recorded_at_utc",
        "device_model",
        "depth_enabled",
        "kind",
        "ar_m",
        "reference_m",
        "signed_error_m",
        "absolute_error_m",
        "relative_error_pct",
        "conditions",
    )

    fun encode(trials: List<FieldTrial>): String = buildString {
        appendLine(header.joinToString(","))
        val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        trials.forEach { trial ->
            appendLine(
                listOf(
                    trial.id,
                    timestampFormat.format(Date(trial.recordedAtEpochMillis)),
                    trial.deviceModel,
                    trial.depthEnabled.toString(),
                    trial.kind.name,
                    trial.arMeters.format(4),
                    trial.referenceMeters.format(4),
                    trial.signedErrorMeters.format(4),
                    trial.absoluteErrorMeters.format(4),
                    (trial.relativeError * 100).format(2),
                    trial.conditions.sortedBy { it.ordinal }.joinToString(";") { it.name },
                ).joinToString(",") { it.csvEscaped() },
            )
        }
    }

    private fun Double.format(decimals: Int): String =
        String.format(Locale.US, "%.${decimals}f", this)

    private fun String.csvEscaped(): String =
        if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + replace("\"", "\"\"") + "\""
        } else {
            this
        }
}
