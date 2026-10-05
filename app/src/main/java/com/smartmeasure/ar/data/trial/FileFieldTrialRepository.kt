package com.smartmeasure.ar.data.trial

import com.smartmeasure.ar.domain.model.FieldTrial
import com.smartmeasure.ar.domain.repository.FieldTrialRepository
import com.smartmeasure.ar.domain.repository.TrialStorageStatus
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
    /** Timestamp for the backup name of an unreadable file. */
    private val clock: () -> Long = System::currentTimeMillis,
    /** Renames without replacing; injectable so the failure path can be tested. */
    private val moveFile: (from: File, to: File) -> Boolean = { from, to -> from.renameTo(to) },
) : FieldTrialRepository {
    private val mutex = Mutex()
    private val trials = MutableStateFlow<List<FieldTrial>?>(null)

    /** Null until the file is first read; written only while holding [mutex]. */
    private val status = MutableStateFlow<TrialStorageStatus?>(null)

    override fun observeTrials(): Flow<List<FieldTrial>> =
        trials
            .onStart { mutex.withLock { loadLocked() } }
            .filterNotNull()

    override fun observeStorageStatus(): Flow<TrialStorageStatus> =
        status
            .onStart { mutex.withLock { loadLocked() } }
            .filterNotNull()

    /**
     * Moves an unreadable file to `<name>.unreadable-<epochMillis>.bak` in the same directory (never
     * deleting or overwriting anything), then allows writes again with an empty list. The original
     * stays in place and writes stay blocked when the move fails.
     */
    override suspend fun recoverUnreadableStorage() {
        mutex.withLock {
            loadLocked()
            if (status.value != TrialStorageStatus.UNREADABLE) return
            withContext(ioDispatcher) {
                val backup = backupFile()
                check(moveFile(file, backup) && !file.exists()) {
                    "Could not move the unreadable field trial file aside."
                }
            }
            trials.value = emptyList()
            status.value = TrialStorageStatus.OK
        }
    }

    /** First backup name that does not exist yet, so an earlier backup is never replaced. */
    private fun backupFile(): File {
        val base = "${file.name}.unreadable-${clock()}"
        return generateSequence(0) { it + 1 }
            .map { n -> File(file.parentFile, if (n == 0) "$base.bak" else "$base-$n.bak") }
            .first { !it.exists() }
    }

    override suspend fun add(trial: FieldTrial) = mutate { current ->
        (listOf(trial) + current.filterNot { it.id == trial.id })
            .sortedByDescending { it.recordedAtEpochMillis }
    }

    override suspend fun delete(id: String) = mutate { current ->
        current.filterNot { it.id == id }
    }

    private suspend fun mutate(transform: (List<FieldTrial>) -> List<FieldTrial>) {
        mutex.withLock {
            val current = loadLocked()
            check(status.value == TrialStorageStatus.OK) {
                "Field trial file has an unsupported format version; refusing to overwrite it."
            }
            val updated = transform(current)
            withContext(ioDispatcher) { write(updated) }
            trials.value = updated
        }
    }

    private suspend fun loadLocked(): List<FieldTrial> =
        trials.value ?: withContext(ioDispatcher) { read() }
            .sortedByDescending { it.recordedAtEpochMillis }
            .also { trials.value = it }

    /**
     * v1 and v2 files are read (v1 is rewritten as v2 on the next write). A file with any other
     * header — typically written by a newer app version — shows no trials, is reported as
     * [TrialStorageStatus.UNREADABLE] and is never overwritten: writes fail with
     * [IllegalStateException], which the caller reports.
     */
    private fun read(): List<FieldTrial> {
        if (!file.exists()) {
            status.value = TrialStorageStatus.OK
            return emptyList()
        }
        val text = file.readText()
        status.value = if (FieldTrialCodec.canOverwrite(text)) {
            TrialStorageStatus.OK
        } else {
            TrialStorageStatus.UNREADABLE
        }
        return FieldTrialCodec.decode(text)
    }

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
