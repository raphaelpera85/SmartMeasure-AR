package com.smartmeasure.ar.domain.repository

import com.smartmeasure.ar.domain.model.FieldTrial
import kotlinx.coroutines.flow.Flow

interface FieldTrialRepository {
    /** Emits all stored trials, newest first. */
    fun observeTrials(): Flow<List<FieldTrial>>

    suspend fun add(trial: FieldTrial)

    suspend fun delete(id: String)
}
