package com.smartmeasure.ar.presentation.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.smartmeasure.ar.domain.model.ArAvailability
import com.smartmeasure.ar.domain.model.ArPreparationResult
import com.smartmeasure.ar.domain.repository.ArAvailabilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DiagnosticsViewModel(
    private val availabilityRepository: ArAvailabilityRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        refreshAvailability()
    }

    fun refreshAvailability() {
        _uiState.update {
            it.copy(
                arAvailability = availabilityRepository.currentAvailability(),
                failure = null,
            )
        }
    }

    fun onPreparationStarted() {
        _uiState.update {
            it.copy(
                preparationState = PreparationState.PREPARING,
                failure = null,
            )
        }
    }

    fun onCameraPermissionDenied() {
        _uiState.update {
            it.copy(
                preparationState = PreparationState.PERMISSION_DENIED,
                failure = null,
            )
        }
    }

    fun onPreparationResult(result: ArPreparationResult) {
        _uiState.update { current ->
            when (result) {
                ArPreparationResult.InstallRequested -> current.copy(
                    preparationState = PreparationState.INSTALL_REQUESTED,
                    failure = null,
                )

                is ArPreparationResult.Ready -> current.copy(
                    arAvailability = ArAvailability.READY,
                    depthSupport = result.depthSupport,
                    planeFindingSupport = result.planeFindingSupport,
                    preparationState = PreparationState.READY,
                    failure = null,
                )

                is ArPreparationResult.Failed -> current.copy(
                    preparationState = PreparationState.FAILED,
                    failure = result.reason,
                )
            }
        }
    }

    class Factory(
        private val availabilityRepository: ArAvailabilityRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DiagnosticsViewModel::class.java))
            return DiagnosticsViewModel(availabilityRepository) as T
        }
    }
}

