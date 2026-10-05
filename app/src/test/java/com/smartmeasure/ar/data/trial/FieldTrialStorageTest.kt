package com.smartmeasure.ar.data.trial

import com.smartmeasure.ar.domain.model.ArSessionSummary
import com.smartmeasure.ar.domain.model.CaptureCondition
import com.smartmeasure.ar.domain.model.FieldTrial
import com.smartmeasure.ar.domain.model.MeasurementKind
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import java.nio.file.Files

private fun sample(id: String, at: Long) = FieldTrial(
    id = id,
    recordedAtEpochMillis = at,
    deviceModel = "Acme\tPhone",
    depthEnabled = id.length % 2 == 0,
    kind = MeasurementKind.HEIGHT,
    arMeters = 2.613,
    referenceMeters = 2.6,
    conditions = setOf(CaptureCondition.SMALL_ROOM, CaptureCondition.OBSTRUCTED),
)

private val session = ArSessionSummary(
    distanceMeters = 3.1415926535,
    trackingLosses = 2,
    durationSeconds = 41.123456789,
    trackingSeconds = 38.5,
    horizontalPlanes = 1,
    verticalPlanes = 3,
)

/** A v1 file exactly as the previous app version wrote it (8 tab-separated fields per line). */
private const val V1_FILE =
    "smartmeasure-field-trials\tv1\n" +
        "b\t20\tPixel 7\tfalse\tWALL\t3.052\t3.0\tLOW_LIGHT,PLAIN_SURFACE\n" +
        "a\t10\tGalaxy S23\ttrue\tOPENING\t0.81\t0.8\t\n"

private val V1_TRIALS = listOf(
    FieldTrial(
        id = "b",
        recordedAtEpochMillis = 20L,
        deviceModel = "Pixel 7",
        depthEnabled = false,
        kind = MeasurementKind.WALL,
        arMeters = 3.052,
        referenceMeters = 3.0,
        conditions = setOf(CaptureCondition.LOW_LIGHT, CaptureCondition.PLAIN_SURFACE),
    ),
    FieldTrial(
        id = "a",
        recordedAtEpochMillis = 10L,
        deviceModel = "Galaxy S23",
        depthEnabled = true,
        kind = MeasurementKind.OPENING,
        arMeters = 0.81,
        referenceMeters = 0.8,
    ),
)

/** v2 line: the 8 v1 fields followed by the 6 session fields. */
private fun v2Line(id: String, sessionFields: String) =
    "$id\t1\tPhone\ttrue\tWALL\t2.0\t2.1\tLOW_LIGHT\t$sessionFields\n"

class FieldTrialCodecTest : StringSpec({
    "round trip keeps every field and sanitizes separators" {
        val original = listOf(sample("a", 1L), sample("bb", 2L))

        val decoded = FieldTrialCodec.decode(FieldTrialCodec.encode(original))

        decoded shouldBe original.map { it.copy(deviceModel = "Acme Phone") }
    }

    "encode writes the v2 header and empty session fields when there is no session" {
        val lines = FieldTrialCodec.encode(listOf(sample("a", 1L))).lines()

        lines[0] shouldBe "smartmeasure-field-trials\tv2"
        lines[1].split('\t').size shouldBe 14
        lines[1].split('\t').takeLast(6) shouldBe List(6) { "" }
    }

    "round trip keeps the session summary exactly" {
        val original = listOf(sample("a", 1L).copy(session = session), sample("bb", 2L))

        val decoded = FieldTrialCodec.decode(FieldTrialCodec.encode(original))

        decoded.map { it.session } shouldBe listOf(session, null)
        decoded shouldBe original.map { it.copy(deviceModel = "Acme Phone") }
    }

    "a real v1 file is read with every field and no session" {
        FieldTrialCodec.decode(V1_FILE) shouldBe V1_TRIALS
    }

    "an invalid session drops only the session and keeps the trial" {
        val text = "smartmeasure-field-trials\tv2\n" +
            // tracking time above duration: rejected by ArSessionSummary.init
            v2Line("over", "1.0\t0\t10.0\t11.0\t0\t0") +
            // negative distance
            v2Line("negative", "-1.0\t0\t10.0\t5.0\t0\t0") +
            // not a number
            v2Line("text", "abc\t0\t10.0\t5.0\t0\t0") +
            // non-finite
            v2Line("nan", "NaN\t0\t10.0\t5.0\t0\t0") +
            // partially filled session
            v2Line("partial", "1.0\t\t10.0\t5.0\t0\t0") +
            v2Line("valid", "1.5\t1\t10.0\t5.0\t2\t0")

        val decoded = FieldTrialCodec.decode(text)

        decoded.map { it.id } shouldBe listOf("over", "negative", "text", "nan", "partial", "valid")
        decoded.map { it.session } shouldBe List(5) { null } +
            ArSessionSummary(1.5, 1, 10.0, 5.0, 2, 0)
        decoded.all { it.arMeters == 2.0 && it.referenceMeters == 2.1 } shouldBe true
    }

    "corrupt lines are skipped without losing valid ones" {
        val text = FieldTrialCodec.encode(listOf(sample("a", 1L))) +
            "broken\tline\n" +
            "x\t1\tPhone\ttrue\tUNKNOWN_KIND\t1.0\t1.0\t\t\t\t\t\t\t\n" +
            "y\t1\tPhone\ttrue\tWALL\t-1.0\t1.0\t\t\t\t\t\t\t\n" +
            // a v1-shaped line inside a v2 file has the wrong field count
            "z\t1\tPhone\ttrue\tWALL\t1.0\t1.0\t\n"

        FieldTrialCodec.decode(text).map { it.id } shouldBe listOf("a")
    }

    "unknown header yields empty list" {
        FieldTrialCodec.decode("something-else\nfoo") shouldBe emptyList()
        FieldTrialCodec.decode("smartmeasure-field-trials\tv3\nfoo") shouldBe emptyList()
        FieldTrialCodec.decode("") shouldBe emptyList()
    }

    "windows line endings are tolerated" {
        val text = FieldTrialCodec.encode(listOf(sample("a", 1L).copy(session = session)))
            .replace("\n", "\r\n")
        FieldTrialCodec.decode(text).single().session shouldBe session
        FieldTrialCodec.decode(V1_FILE.replace("\n", "\r\n")) shouldBe V1_TRIALS
    }
})

class FileFieldTrialRepositoryTest : StringSpec({
    "trials persist across instances, newest first, and can be deleted" {
        val dir = Files.createTempDirectory("trials").toFile()
        try {
            val file = dir.resolve("field_trials.tsv")
            val first = FileFieldTrialRepository(file)
            first.observeTrials().first() shouldBe emptyList()

            first.add(sample("old", 10L))
            first.add(sample("new", 20L))
            first.observeTrials().first().map { it.id } shouldBe listOf("new", "old")

            val reopened = FileFieldTrialRepository(file)
            reopened.observeTrials().first().map { it.id } shouldBe listOf("new", "old")

            reopened.delete("new")
            FileFieldTrialRepository(file).observeTrials().first().map { it.id } shouldBe listOf("old")
            dir.resolve("field_trials.tsv.tmp").exists() shouldBe false
        } finally {
            dir.deleteRecursively()
        }
    }

    "adding an existing id replaces it instead of duplicating" {
        val dir = Files.createTempDirectory("trials").toFile()
        try {
            val repository = FileFieldTrialRepository(dir.resolve("t.tsv"))
            repository.add(sample("a", 1L))
            repository.add(sample("a", 1L).copy(arMeters = 2.7))

            val stored = repository.observeTrials().first()
            stored.map { it.arMeters } shouldBe listOf(2.7)
        } finally {
            dir.deleteRecursively()
        }
    }

    "a v1 file is migrated to v2 on the first write without losing trials" {
        val dir = Files.createTempDirectory("trials").toFile()
        try {
            val file = dir.resolve("t.tsv")
            file.writeText(V1_FILE)
            val repository = FileFieldTrialRepository(file)
            repository.observeTrials().first() shouldBe V1_TRIALS

            val withSession = sample("c", 30L).copy(deviceModel = "Pixel 7", session = session)
            repository.add(withSession)

            file.readText().lines().first() shouldBe "smartmeasure-field-trials\tv2"
            FileFieldTrialRepository(file).observeTrials().first() shouldBe
                listOf(withSession) + V1_TRIALS
        } finally {
            dir.deleteRecursively()
        }
    }

    "a file from a newer version is never overwritten and the write is reported as failed" {
        val dir = Files.createTempDirectory("trials").toFile()
        try {
            val file = dir.resolve("t.tsv")
            val future = "smartmeasure-field-trials\tv3\nsome\tfuture\tline\n"
            file.writeText(future)
            val repository = FileFieldTrialRepository(file)
            repository.observeTrials().first() shouldBe emptyList()

            shouldThrow<IllegalStateException> { repository.add(sample("a", 1L)) }
            shouldThrow<IllegalStateException> { repository.delete("some") }

            file.readText() shouldBe future
            repository.observeTrials().first() shouldBe emptyList()
            dir.resolve("t.tsv.tmp").exists() shouldBe false
        } finally {
            dir.deleteRecursively()
        }
    }
})
