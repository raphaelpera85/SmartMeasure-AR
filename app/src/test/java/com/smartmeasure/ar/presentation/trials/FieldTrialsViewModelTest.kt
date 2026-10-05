package com.smartmeasure.ar.presentation.trials

import com.smartmeasure.ar.domain.model.ArSessionSummary
import com.smartmeasure.ar.domain.model.CaptureCondition
import com.smartmeasure.ar.domain.model.FieldTrial
import com.smartmeasure.ar.domain.model.MeasurementKind
import com.smartmeasure.ar.domain.repository.FieldTrialRepository
import com.smartmeasure.ar.domain.repository.TrialStorageStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.util.Locale

/** In-memory fake: the real file repository has its own tests. */
private class FakeFieldTrialRepository(
    initial: List<FieldTrial> = emptyList(),
    var failWrites: Boolean = false,
    initialStatus: TrialStorageStatus = TrialStorageStatus.OK,
    var failRecovery: Boolean = false,
) : FieldTrialRepository {
    val trials = MutableStateFlow(initial)
    val status = MutableStateFlow(initialStatus)
    var recoverCalls = 0

    override fun observeTrials(): Flow<List<FieldTrial>> = trials

    override suspend fun add(trial: FieldTrial) {
        if (failWrites) error("disk full")
        trials.value = listOf(trial) + trials.value
    }

    override suspend fun delete(id: String) {
        if (failWrites) error("disk full")
        trials.value = trials.value.filterNot { it.id == id }
    }

    override fun observeStorageStatus(): Flow<TrialStorageStatus> = status

    override suspend fun recoverUnreadableStorage() {
        recoverCalls++
        if (failRecovery) error("rename refused")
        if (status.value == TrialStorageStatus.UNREADABLE) {
            trials.value = emptyList()
            status.value = TrialStorageStatus.OK
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FieldTrialsViewModelTest : StringSpec({
    lateinit var defaultLocale: Locale

    beforeTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }
    afterTest {
        Dispatchers.resetMain()
        Locale.setDefault(defaultLocale)
    }

    fun viewModel(repository: FieldTrialRepository) = FieldTrialsViewModel(
        repository = repository,
        deviceModel = "Acme One",
        clock = { 42L },
        idFactory = { "trial-1" },
    )

    "stored trials and their summaries reach the ui state" {
        val stored = FieldTrial("a", 1L, "Acme One", false, MeasurementKind.WALL, 3.02, 3.0)
        val vm = viewModel(FakeFieldTrialRepository(listOf(stored)))

        vm.uiState.value.loading shouldBe false
        vm.uiState.value.trials shouldBe listOf(stored)
        vm.uiState.value.summaries.single().count shouldBe 1
    }

    "a captured AR distance prefills the draft and clears the reference" {
        val vm = viewModel(FakeFieldTrialRepository())
        vm.onReferenceChanged("9")

        vm.startDraft(arMeters = 2.4567, depthEnabled = true, session = null)

        vm.uiState.value.draft.arInput shouldBe "2.457"
        vm.uiState.value.draft.referenceInput shouldBe ""
        vm.uiState.value.draft.depthEnabled shouldBe true
    }

    "a trial recorded from the AR screen keeps the session summary captured then" {
        val repository = FakeFieldTrialRepository()
        val vm = viewModel(repository)
        val session = ArSessionSummary(4.2, 1, 25.0, 24.0, 1, 2)

        vm.startDraft(arMeters = 2.4567, depthEnabled = true, session = session)
        vm.uiState.value.draft.session shouldBe session
        vm.onReferenceChanged("2.45")
        vm.save()

        repository.trials.value.single().session shouldBe session
    }

    "manually editing the AR value keeps the session it came from" {
        val repository = FakeFieldTrialRepository()
        val vm = viewModel(repository)
        val session = ArSessionSummary(4.2, 1, 25.0, 24.0, 1, 2)
        vm.startDraft(arMeters = 2.4567, depthEnabled = true, session = session)

        vm.onArChanged("2.46")
        vm.onReferenceChanged("2.45")
        vm.save()

        repository.trials.value.single().session shouldBe session
    }

    "after saving, the next trial does not inherit the previous session" {
        val repository = FakeFieldTrialRepository()
        val vm = viewModel(repository)
        vm.startDraft(arMeters = 2.0, depthEnabled = false, session = ArSessionSummary(1.0, 0, 5.0, 5.0, 0, 1))
        vm.onReferenceChanged("2")
        vm.save()

        vm.uiState.value.draft.session shouldBe null
    }

    "a draft started without AR has no session even if a previous draft had one" {
        val vm = viewModel(FakeFieldTrialRepository())
        vm.startDraft(arMeters = 2.0, depthEnabled = false, session = ArSessionSummary(1.0, 0, 5.0, 5.0, 0, 1))

        vm.startDraft(arMeters = null, depthEnabled = false, session = null)

        vm.uiState.value.draft.session shouldBe null
    }

    "saving a valid draft stores it and keeps kind, depth and conditions" {
        val repository = FakeFieldTrialRepository()
        val vm = viewModel(repository)
        vm.startDraft(arMeters = null, depthEnabled = true, session = null)
        vm.onKindSelected(MeasurementKind.HEIGHT)
        vm.onConditionToggled(CaptureCondition.LOW_LIGHT)
        vm.onArChanged("2,61")
        vm.onReferenceChanged("2.60")

        vm.save()

        repository.trials.value shouldBe listOf(
            FieldTrial(
                id = "trial-1",
                recordedAtEpochMillis = 42L,
                deviceModel = "Acme One",
                depthEnabled = true,
                kind = MeasurementKind.HEIGHT,
                arMeters = 2.61,
                referenceMeters = 2.60,
                conditions = setOf(CaptureCondition.LOW_LIGHT),
            ),
        )
        val state = vm.uiState.value
        state.justSaved shouldBe true
        state.draft shouldBe TrialDraft(
            kind = MeasurementKind.HEIGHT,
            depthEnabled = true,
            conditions = setOf(CaptureCondition.LOW_LIGHT),
        )
    }

    "an invalid draft is not stored and reports the field" {
        val repository = FakeFieldTrialRepository()
        val vm = viewModel(repository)
        vm.onArChanged("3")
        vm.onReferenceChanged("")

        vm.save()

        repository.trials.value shouldBe emptyList()
        vm.uiState.value.inputError shouldBe TrialInputError.INVALID_REFERENCE
    }

    "editing the draft clears a previous error" {
        val vm = viewModel(FakeFieldTrialRepository())
        vm.save()
        vm.uiState.value.inputError shouldBe TrialInputError.INVALID_AR

        vm.onArChanged("1")

        vm.uiState.value.inputError shouldBe null
    }

    "toggling a condition twice removes it" {
        val vm = viewModel(FakeFieldTrialRepository())
        vm.onConditionToggled(CaptureCondition.REFLECTIVE_SURFACE)
        vm.onConditionToggled(CaptureCondition.REFLECTIVE_SURFACE)

        vm.uiState.value.draft.conditions shouldBe emptySet()
    }

    "storage failures are surfaced instead of reported as saved" {
        val vm = viewModel(FakeFieldTrialRepository(failWrites = true))
        vm.onArChanged("1")
        vm.onReferenceChanged("1")

        vm.save()

        vm.uiState.value.saveFailed shouldBe true
        vm.uiState.value.justSaved shouldBe false
    }

    "delete removes the trial" {
        val stored = FieldTrial("a", 1L, "Acme One", false, MeasurementKind.WALL, 3.02, 3.0)
        val repository = FakeFieldTrialRepository(listOf(stored))
        val vm = viewModel(repository)

        vm.delete("a")

        vm.uiState.value.trials shouldBe emptyList()
    }

    "a failed delete is surfaced as saveFailed and keeps the trial" {
        val stored = FieldTrial("a", 1L, "Acme One", false, MeasurementKind.WALL, 3.02, 3.0)
        val vm = viewModel(FakeFieldTrialRepository(listOf(stored), failWrites = true))

        vm.delete("a")

        vm.uiState.value.saveFailed shouldBe true
        vm.uiState.value.trials shouldBe listOf(stored)
    }

    "an unreadable storage is reflected in the ui state" {
        viewModel(FakeFieldTrialRepository()).uiState.value.storageUnreadable shouldBe false

        val vm = viewModel(FakeFieldTrialRepository(initialStatus = TrialStorageStatus.UNREADABLE))

        vm.uiState.value.storageUnreadable shouldBe true
    }

    "recoverStorage asks the repository to recover and clears storageUnreadable" {
        val repository = FakeFieldTrialRepository(initialStatus = TrialStorageStatus.UNREADABLE)
        val vm = viewModel(repository)

        vm.recoverStorage()

        repository.recoverCalls shouldBe 1
        vm.uiState.value.storageUnreadable shouldBe false
        vm.uiState.value.recoveryFailed shouldBe false
    }

    "a failed recovery is reported and storage stays unreadable" {
        val repository = FakeFieldTrialRepository(initialStatus = TrialStorageStatus.UNREADABLE, failRecovery = true)
        val vm = viewModel(repository)

        vm.recoverStorage()

        vm.uiState.value.recoveryFailed shouldBe true
        vm.uiState.value.storageUnreadable shouldBe true
    }

    "a new recovery attempt clears the previous recovery failure" {
        val repository = FakeFieldTrialRepository(initialStatus = TrialStorageStatus.UNREADABLE, failRecovery = true)
        val vm = viewModel(repository)
        vm.recoverStorage()

        repository.failRecovery = false
        vm.recoverStorage()

        vm.uiState.value.recoveryFailed shouldBe false
        vm.uiState.value.storageUnreadable shouldBe false
    }

    "export produces csv for the current trials" {
        val stored = FieldTrial("a", 1L, "Acme One", false, MeasurementKind.WALL, 3.02, 3.0)
        val vm = viewModel(FakeFieldTrialRepository(listOf(stored)))

        vm.exportCsv() shouldContain "a,1970-01-01T00:00:00Z,Acme One,false,WALL,3.0200,3.0000"
    }
})
