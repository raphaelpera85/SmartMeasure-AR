package com.smartmeasure.ar.data.trial

import com.smartmeasure.ar.domain.model.CaptureCondition
import com.smartmeasure.ar.domain.model.FieldTrial
import com.smartmeasure.ar.domain.model.MeasurementKind
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

class FieldTrialCodecTest : StringSpec({
    "round trip keeps every field and sanitizes separators" {
        val original = listOf(sample("a", 1L), sample("bb", 2L))

        val decoded = FieldTrialCodec.decode(FieldTrialCodec.encode(original))

        decoded shouldBe original.map { it.copy(deviceModel = "Acme Phone") }
    }

    "corrupt lines are skipped without losing valid ones" {
        val text = FieldTrialCodec.encode(listOf(sample("a", 1L))) +
            "broken\tline\n" +
            "x\t1\tPhone\ttrue\tUNKNOWN_KIND\t1.0\t1.0\t\n" +
            "y\t1\tPhone\ttrue\tWALL\t-1.0\t1.0\t\n"

        FieldTrialCodec.decode(text).map { it.id } shouldBe listOf("a")
    }

    "unknown header yields empty list" {
        FieldTrialCodec.decode("something-else\nfoo") shouldBe emptyList()
        FieldTrialCodec.decode("") shouldBe emptyList()
    }

    "windows line endings are tolerated" {
        val text = FieldTrialCodec.encode(listOf(sample("a", 1L))).replace("\n", "\r\n")
        FieldTrialCodec.decode(text).map { it.id } shouldBe listOf("a")
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
})
