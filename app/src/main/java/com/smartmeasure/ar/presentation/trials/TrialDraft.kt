package com.smartmeasure.ar.presentation.trials

import com.smartmeasure.ar.domain.model.ArSessionSummary
import com.smartmeasure.ar.domain.model.CaptureCondition
import com.smartmeasure.ar.domain.model.FieldTrial
import com.smartmeasure.ar.domain.model.MeasurementKind

enum class TrialInputError {
    INVALID_AR,
    INVALID_REFERENCE,
}

/**
 * Editable form values for a new trial. Text inputs accept either ',' or '.' as decimal mark.
 *
 * [session] is the AR session summary captured with the measurement that prefilled [arInput]; it
 * is kept if the user edits [arInput] by hand (rounding or correcting a reading does not change the
 * session it was taken in) and is null for drafts not started from the AR screen.
 */
data class TrialDraft(
    val arInput: String = "",
    val referenceInput: String = "",
    val kind: MeasurementKind = MeasurementKind.WALL,
    val depthEnabled: Boolean = false,
    val conditions: Set<CaptureCondition> = emptySet(),
    val session: ArSessionSummary? = null,
) {
    sealed interface Result {
        data class Valid(val trial: FieldTrial) : Result
        data class Invalid(val error: TrialInputError) : Result
    }

    fun toTrial(id: String, recordedAtEpochMillis: Long, deviceModel: String): Result {
        val ar = arInput.toMetersOrNull()
            ?: return Result.Invalid(TrialInputError.INVALID_AR)
        val reference = referenceInput.toMetersOrNull()
            ?: return Result.Invalid(TrialInputError.INVALID_REFERENCE)
        return Result.Valid(
            FieldTrial(
                id = id,
                recordedAtEpochMillis = recordedAtEpochMillis,
                deviceModel = deviceModel,
                depthEnabled = depthEnabled,
                kind = kind,
                arMeters = ar,
                referenceMeters = reference,
                conditions = conditions,
                session = session,
            ),
        )
    }

    private companion object {
        /** Upper bound that catches typos (e.g. centimeters typed as meters) for residential rooms. */
        const val MAX_METERS = 100.0

        fun String.toMetersOrNull(): Double? =
            trim()
                .replace(',', '.')
                .toDoubleOrNull()
                ?.takeIf { it.isFinite() && it > 0.0 && it <= MAX_METERS }
    }
}
