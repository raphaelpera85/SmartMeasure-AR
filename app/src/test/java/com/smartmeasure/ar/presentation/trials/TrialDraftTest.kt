package com.smartmeasure.ar.presentation.trials

import com.smartmeasure.ar.domain.model.ArSessionSummary
import com.smartmeasure.ar.domain.model.CaptureCondition
import com.smartmeasure.ar.domain.model.MeasurementKind
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TrialDraftTest : StringSpec({
    fun TrialDraft.convert() = toTrial(id = "id", recordedAtEpochMillis = 5L, deviceModel = "Phone")

    "accepts comma or dot decimals and keeps form choices" {
        val result = TrialDraft(
            arInput = " 3,052 ",
            referenceInput = "3.000",
            kind = MeasurementKind.OPENING,
            depthEnabled = true,
            conditions = setOf(CaptureCondition.LOW_LIGHT),
        ).convert()

        val trial = result.shouldBeInstanceOf<TrialDraft.Result.Valid>().trial
        trial.arMeters shouldBe 3.052
        trial.referenceMeters shouldBe 3.0
        trial.kind shouldBe MeasurementKind.OPENING
        trial.depthEnabled shouldBe true
        trial.conditions shouldBe setOf(CaptureCondition.LOW_LIGHT)
        trial.recordedAtEpochMillis shouldBe 5L
        trial.deviceModel shouldBe "Phone"
    }

    "the AR session summary is carried into the trial" {
        val session = ArSessionSummary(2.5, 1, 30.0, 28.0, 1, 2)

        val trial = TrialDraft(arInput = "2", referenceInput = "2", session = session).convert()
            .shouldBeInstanceOf<TrialDraft.Result.Valid>().trial

        trial.session shouldBe session
    }

    "a draft without AR session yields a trial without session" {
        TrialDraft(arInput = "2", referenceInput = "2").convert()
            .shouldBeInstanceOf<TrialDraft.Result.Valid>().trial.session shouldBe null
    }

    "reports which field is invalid" {
        TrialDraft(arInput = "", referenceInput = "1").convert() shouldBe
            TrialDraft.Result.Invalid(TrialInputError.INVALID_AR)
        TrialDraft(arInput = "1", referenceInput = "abc").convert() shouldBe
            TrialDraft.Result.Invalid(TrialInputError.INVALID_REFERENCE)
        TrialDraft(arInput = "0", referenceInput = "1").convert() shouldBe
            TrialDraft.Result.Invalid(TrialInputError.INVALID_AR)
    }

    "rejects values above the residential sanity limit" {
        TrialDraft(arInput = "1", referenceInput = "305").convert() shouldBe
            TrialDraft.Result.Invalid(TrialInputError.INVALID_REFERENCE)
        TrialDraft(arInput = "NaN", referenceInput = "1").convert() shouldBe
            TrialDraft.Result.Invalid(TrialInputError.INVALID_AR)
    }
})
