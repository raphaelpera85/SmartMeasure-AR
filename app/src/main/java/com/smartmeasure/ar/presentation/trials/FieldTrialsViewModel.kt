package com.smartmeasure.ar.presentation.trials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(FieldTrialsUiState())
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
                        // Kind, Depth and conditions usually repeat across consecutive trials; the
                        // session belongs to the saved measurement only.
                        _uiState.update {
                            it.copy(
                                draft = it.draft.copy(arInput = "", referenceInput = "", session = null),
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
            it.copy(
                draft = transform(it.draft),
                inputError = null,
                saveFailed = false,
                justSaved = false,
            )
        }
    }

    class Factory(
        private val repository: FieldTrialRepository,
        private val deviceModel: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(FieldTrialsViewModel::class.java))
            return FieldTrialsViewModel(repository, deviceModel) as T
        }
    }
}
