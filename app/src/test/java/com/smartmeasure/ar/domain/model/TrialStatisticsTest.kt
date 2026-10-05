package com.smartmeasure.ar.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private fun trial(
    ar: Double,
    reference: Double,
    kind: MeasurementKind = MeasurementKind.WALL,
    depth: Boolean = false,
    id: String = "t-$ar-$reference-$kind-$depth",
) = FieldTrial(
    id = id,
    recordedAtEpochMillis = 0L,
    deviceModel = "Test Phone",
    depthEnabled = depth,
    kind = kind,
    arMeters = ar,
    referenceMeters = reference,
)

class FieldTrialTest : StringSpec({
    "errors are signed against the reference" {
        val over = trial(ar = 3.05, reference = 3.00)
        over.signedErrorMeters shouldBe (0.05 plusOrMinus 1e-12)
        over.absoluteErrorMeters shouldBe (0.05 plusOrMinus 1e-12)
        over.relativeError shouldBe (0.05 / 3.0 plusOrMinus 1e-12)

        val under = trial(ar = 2.90, reference = 3.00)
        under.signedErrorMeters shouldBe (-0.10 plusOrMinus 1e-12)
        under.absoluteErrorMeters shouldBe (0.10 plusOrMinus 1e-12)
    }

    "non-positive or non-finite values are rejected" {
        shouldThrow<IllegalArgumentException> { trial(ar = 0.0, reference = 1.0) }
        shouldThrow<IllegalArgumentException> { trial(ar = 1.0, reference = -1.0) }
        shouldThrow<IllegalArgumentException> { trial(ar = Double.NaN, reference = 1.0) }
        shouldThrow<IllegalArgumentException> { trial(ar = 1.0, reference = Double.POSITIVE_INFINITY) }
    }
})

class TrialStatisticsTest : StringSpec({
    "groups never mix measurement kind or depth" {
        val summaries = TrialStatistics.summarize(
            listOf(
                trial(3.02, 3.00, MeasurementKind.WALL, depth = true),
                trial(3.10, 3.00, MeasurementKind.WALL, depth = false),
                trial(0.85, 0.80, MeasurementKind.OPENING, depth = false),
                trial(2.95, 3.00, MeasurementKind.WALL, depth = true),
            ),
        )

        summaries shouldHaveSize 3
        summaries.map { it.kind to it.depthEnabled } shouldBe listOf(
            MeasurementKind.WALL to false,
            MeasurementKind.WALL to true,
            MeasurementKind.OPENING to false,
        )
        val wallWithDepth = summaries[1]
        wallWithDepth.count shouldBe 2
        wallWithDepth.meanAbsoluteErrorMeters shouldBe (0.035 plusOrMinus 1e-9)
        wallWithDepth.meanSignedErrorMeters shouldBe (-0.015 plusOrMinus 1e-9)
        wallWithDepth.maxAbsoluteErrorMeters shouldBe (0.05 plusOrMinus 1e-9)
    }

    "median handles odd and even sample counts" {
        TrialStatistics.median(listOf(1.0, 2.0, 9.0)) shouldBe 2.0
        TrialStatistics.median(listOf(1.0, 2.0, 4.0, 9.0)) shouldBe 3.0
    }

    "p90 uses nearest rank" {
        val values = (1..10).map { it.toDouble() }
        TrialStatistics.nearestRankPercentile(values, 0.90) shouldBe 9.0
        TrialStatistics.nearestRankPercentile(listOf(0.5, 0.1).sorted(), 0.90) shouldBe 0.5
        TrialStatistics.nearestRankPercentile(listOf(0.2), 0.90) shouldBe 0.2
    }

    "summary of errors is computed on absolute values" {
        val summary = TrialStatistics.summarize(
            listOf(trial(1.1, 1.0), trial(0.9, 1.0), trial(1.0, 1.0)),
        ).single()

        summary.medianAbsoluteErrorMeters shouldBe (0.1 plusOrMinus 1e-9)
        summary.meanSignedErrorMeters shouldBe (0.0 plusOrMinus 1e-9)
        summary.meanRelativeError shouldBe (0.2 / 3 plusOrMinus 1e-9)
    }

    "no trials produce no summaries" {
        TrialStatistics.summarize(emptyList()) shouldBe emptyList()
    }

    "minimum representative sample is ten trials" {
        TrialStatistics.MIN_REPRESENTATIVE_SAMPLES shouldBe 10
    }

    "nine trials are not representative" {
        val summary = TrialStatistics.summarize((1..9).map { trial(1.0 + it / 1000.0, 1.0) }).single()
        summary.count shouldBe 9
        summary.isRepresentative shouldBe false
    }

    "ten trials are representative" {
        val summary = TrialStatistics.summarize((1..10).map { trial(1.0 + it / 1000.0, 1.0) }).single()
        summary.count shouldBe 10
        summary.isRepresentative shouldBe true
    }

    "a single trial is not representative" {
        val summary = TrialStatistics.summarize(listOf(trial(1.01, 1.0))).single()
        summary.count shouldBe 1
        summary.isRepresentative shouldBe false
    }
})

class FieldTrialCsvTest : StringSpec({
    "csv uses dot decimals, UTC timestamps and escapes text" {
        val csv = FieldTrialCsv.encode(
            listOf(
                FieldTrial(
                    id = "a",
                    recordedAtEpochMillis = 0L,
                    deviceModel = "Acme \"X\", Pro",
                    depthEnabled = true,
                    kind = MeasurementKind.OPENING,
                    arMeters = 0.8123,
                    referenceMeters = 0.8,
                    conditions = setOf(CaptureCondition.REFLECTIVE_SURFACE, CaptureCondition.LOW_LIGHT),
                ),
            ),
        )

        csv.lines()[0] shouldBe "id,recorded_at_utc,device_model,depth_enabled,kind,ar_m,reference_m," +
            "signed_error_m,absolute_error_m,relative_error_pct,conditions"
        csv.lines()[1] shouldBe "a,1970-01-01T00:00:00Z,\"Acme \"\"X\"\", Pro\",true,OPENING,0.8123,0.8000," +
            "0.0123,0.0123,1.54,LOW_LIGHT;REFLECTIVE_SURFACE"
    }
})
