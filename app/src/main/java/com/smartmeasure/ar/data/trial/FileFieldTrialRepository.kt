package com.smartmeasure.ar.data.trial

import com.smartmeasure.ar.domain.model.FieldTrial
import com.smartmeasure.ar.domain.repository.FieldTrialRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Local-only trial storage in the app's private files directory. Nothing leaves the device unless
 * the user explicitly exports. Writes go to a temporary file first and then replace the original.
 */
class FileFieldTrialRepository(
    private val file: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FieldTrialRepository {
    private val mutex = Mutex()
    private val trials = MutableStateFlow<List<FieldTrial>?>(null)

    override fun observeTrials(): Flow<List<FieldTrial>> =
        trials
            .onStart { mutex.withLock { loadLocked() } }
            .filterNotNull()

    override suspend fun add(trial: FieldTrial) = mutate { current ->
        (listOf(trial) + current.filterNot { it.id == trial.id })
            .sortedByDescending { it.recordedAtEpochMillis }
    }

    override suspend fun delete(id: String) = mutate { current ->
        current.filterNot { it.id == id }
    }

    private suspend fun mutate(transform: (List<FieldTrial>) -> List<FieldTrial>) {
        mutex.withLock {
            val updated = transform(loadLocked())
            withContext(ioDispatcher) { write(updated) }
            trials.value = updated
        }
    }

    private suspend fun loadLocked(): List<FieldTrial> =
        trials.value ?: withContext(ioDispatcher) { read() }
            .sortedByDescending { it.recordedAtEpochMillis }
            .also { trials.value = it }

    private fun read(): List<FieldTrial> =
        if (file.exists()) FieldTrialCodec.decode(file.readText()) else emptyList()

    private fun write(items: List<FieldTrial>) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(FieldTrialCodec.encode(items))
        if (!temp.renameTo(file)) {
            // Some filesystems (e.g. Windows) refuse to rename over an existing file.
            file.delete()
            check(temp.renameTo(file)) { "Could not save field trials." }
        }
    }
}
