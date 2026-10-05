package com.smartmeasure.ar.domain.repository

import com.smartmeasure.ar.domain.model.FieldTrial
import kotlinx.coroutines.flow.Flow

/** Whether the stored trials could be read. */
enum class TrialStorageStatus {
    /** Absent, empty or in a supported format: trials are listed and writes are allowed. */
    OK,

    /**
     * The stored data has a header this version cannot read (a newer app version or a corrupt
     * file). No trials are listed and writes are refused until
     * [FieldTrialRepository.recoverUnreadableStorage].
     */
    UNREADABLE,
}

interface FieldTrialRepository {
    /** Emits all stored trials, newest first. */
    fun observeTrials(): Flow<List<FieldTrial>>

    /** Emits the current storage status, starting once the stored data has been read. */
    fun observeStorageStatus(): Flow<TrialStorageStatus>

    suspend fun add(trial: FieldTrial)

    suspend fun delete(id: String)

    /**
     * When the status is [TrialStorageStatus.UNREADABLE], keeps the unreadable data aside as a
     * backup (never deleted), then allows writes again starting from an empty list. Does nothing
     * when the status is [TrialStorageStatus.OK]. Throws when the backup cannot be made; the
     * status then stays [TrialStorageStatus.UNREADABLE].
     */
    suspend fun recoverUnreadableStorage()
}
