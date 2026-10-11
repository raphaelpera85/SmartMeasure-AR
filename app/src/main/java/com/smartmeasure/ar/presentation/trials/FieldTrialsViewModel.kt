package com.smartmeasure.ar.presentation.trials

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.smartmeasure.ar.domain.model.ArSessionSummary
import com.smartmeasure.ar.domain.model.CaptureCondition
import com.smartmeasure.ar.domain.model.FieldTrial
import com.smartmeasure.ar.domain.model.FieldTrialCsv
import com.smartmeasure.ar.domain.model.MeasurementKind
import com.smartmeasure.ar.domain.model.TrialStatistics
import com.smartmeasure.ar.domain.model.TrialSummary
import com.smartmeasure.ar.domain.repository.FieldTrialRepository
import com.smartmeasure.ar.domain.repository.TrialStorageStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class FieldTrialsUiState(
    val loading: Boolean = true,
    val trials: List<FieldTrial> = emptyList(),
    val summaries: List<TrialSummary> = emptyList(),
    val draft: TrialDraft = TrialDraft(),
    val inputError: TrialInputError? = null,
    val saveFailed: Boolean = false,
    val justSaved: Boolean = false,
    /** The stored trials cannot be read; writes fail until [FieldTrialsViewModel.recoverStorage]. */
    val storageUnreadable: Boolean = false,
    /** The last [FieldTrialsViewModel.recoverStorage] could not move the file aside. */
    val recoveryFailed: Boolean = false,
)

class FieldTrialsViewModel(
    private val repository: FieldTrialRepository,
    private val deviceModel: String,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        FieldTrialsUiState(draft = restoreDraft()),
    )
    val uiState: StateFlow<FieldTrialsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeTrials().collect { trials ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        trials = trials,
                        summaries = TrialStatistics.summarize(trials),
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observeStorageStatus().collect { status ->
                _uiState.update { it.copy(storageUnreadable = status == TrialStorageStatus.UNREADABLE) }
            }
        }
    }

    /**
     * Starts a new draft, optionally prefilled with an AR measurement just captured and the summary
     * of the AR session it came from ([session] is null when the draft is not started from AR).
     */
    fun startDraft(arMeters: Double?, depthEnabled: Boolean, session: ArSessionSummary?) {
        val arInput = arMeters?.let { String.format(Locale.getDefault(), "%.3f", it) }.orEmpty()
        updateDraft {
            it.copy(arInput = arInput, referenceInput = "", depthEnabled = depthEnabled, session = session)
        }
    }

    fun onArChanged(value: String) = updateDraft { it.copy(arInput = value) }

    fun onReferenceChanged(value: String) = updateDraft { it.copy(referenceInput = value) }

    fun onKindSelected(kind: MeasurementKind) = updateDraft { it.copy(kind = kind) }

    fun onDepthChanged(enabled: Boolean) = updateDraft { it.copy(depthEnabled = enabled) }

    fun onConditionToggled(condition: CaptureCondition) = updateDraft { draft ->
        val conditions = if (condition in draft.conditions) {
            draft.conditions - condition
        } else {
            draft.conditions + condition
        }
        draft.copy(conditions = conditions)
    }

    fun save() {
        val result = _uiState.value.draft.toTrial(idFactory(), clock(), deviceModel)
        when (result) {
            is TrialDraft.Result.Invalid -> _uiState.update {
                it.copy(inputError = result.error, justSaved = false)
            }

            is TrialDraft.Result.Valid -> viewModelScope.launch {
                runCatching { repository.add(result.trial) }
                    .onSuccess {
                        _uiState.update {
                            // Kind, Depth and conditions usually repeat across consecutive trials; the
                            // session belongs to the saved measurement only.
                            val clearedDraft = it.draft.copy(
                                arInput = "",
                                referenceInput = "",
                                session = null,
                            )
                            persistDraft(clearedDraft)
                            it.copy(
                                draft = clearedDraft,
                                justSaved = true,
                            )
                        }
                    }
                    .onFailure { _uiState.update { it.copy(saveFailed = true, justSaved = false) } }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onFailure { _uiState.update { it.copy(saveFailed = true) } }
        }
    }

    fun exportCsv(): String = FieldTrialCsv.encode(_uiState.value.trials)

    fun recoverStorage() {
        _uiState.update { it.copy(recoveryFailed = false) }
        viewModelScope.launch {
            runCatching { repository.recoverUnreadableStorage() }
                .onFailure { _uiState.update { it.copy(recoveryFailed = true) } }
        }
    }

    private fun updateDraft(transform: (TrialDraft) -> TrialDraft) {
        _uiState.update {
            val draft = transform(it.draft)
            persistDraft(draft)
            it.copy(
                draft = draft,
                inputError = null,
                saveFailed = false,
                justSaved = false,
            )
        }
    }

    private fun restoreDraft(): TrialDraft {
        val kind = savedState.get<String>(KEY_KIND)
            ?.let { saved -> MeasurementKind.entries.firstOrNull { it.name == saved } }
            ?: MeasurementKind.WALL
        val conditions = savedState.get<List<String>>(KEY_CONDITIONS)
            .orEmpty()
            .mapNotNull { saved -> CaptureCondition.entries.firstOrNull { it.name == saved } }
            .toSet()
        return TrialDraft(
            arInput = savedState[KEY_AR_INPUT] ?: "",
            referenceInput = savedState[KEY_REFERENCE_INPUT] ?: "",
            kind = kind,
            depthEnabled = savedState[KEY_DEPTH_ENABLED] ?: false,
            conditions = conditions,
        )
    }

    private fun persistDraft(draft: TrialDraft) {
        savedState[KEY_AR_INPUT] = draft.arInput
        savedState[KEY_REFERENCE_INPUT] = draft.referenceInput
        savedState[KEY_KIND] = draft.kind.name
        savedState[KEY_DEPTH_ENABLED] = draft.depthEnabled
        savedState[KEY_CONDITIONS] = draft.conditions.map { it.name }
    }

    class Factory(
        owner: SavedStateRegistryOwner,
        private val repository: FieldTrialRepository,
        private val deviceModel: String,
    ) : AbstractSavedStateViewModelFactory(owner, null) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle,
        ): T {
            require(modelClass.isAssignableFrom(FieldTrialsViewModel::class.java))
            return FieldTrialsViewModel(
                repository = repository,
                deviceModel = deviceModel,
                savedState = handle,
            ) as T
        }
    }

    private companion object {
        const val KEY_AR_INPUT = "field_trials_draft_ar_input"
        const val KEY_REFERENCE_INPUT = "field_trials_draft_reference_input"
        const val KEY_KIND = "field_trials_draft_kind"
        const val KEY_DEPTH_ENABLED = "field_trials_draft_depth_enabled"
        const val KEY_CONDITIONS = "field_trials_draft_conditions"
    }
}
