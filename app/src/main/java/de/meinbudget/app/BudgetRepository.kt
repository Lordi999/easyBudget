package de.meinbudget.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/** Probleme beim Lesen/Schreiben der Daten, die dem Nutzer angezeigt werden. */
sealed interface StorageProblem {
    /** Datei war unlesbar und wurde unter [backupName] beiseitegelegt – nicht überschrieben. */
    data class LoadFailed(val backupName: String) : StorageProblem
    data object SaveFailed : StorageProblem
}

data class ImportResult(val imported: Int, val skipped: Int, val invalid: Int)

/**
 * Speichert alle Monate als JSON-Datei im privaten App-Speicher.
 * Eine Instanz pro Prozess ([get]), damit App und Widget dieselben Daten sehen.
 */
class BudgetRepository private constructor(private val context: Context) {
    private val file = File(context.filesDir, "budget.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()
    private val serializer = ListSerializer(Month.serializer())

    private val _problem = MutableStateFlow<StorageProblem?>(null)
    val problem: StateFlow<StorageProblem?> = _problem.asStateFlow()

    private val _months = MutableStateFlow(load())
    val months: StateFlow<List<Month>> = _months.asStateFlow()

    fun update(transform: (List<Month>) -> List<Month>) {
        _months.update { transform(it).sortedByDescending(Month::key) }
        scope.launch {
            writeLock.withLock {
                try {
                    val tmp = File(file.parentFile, file.name + ".tmp")
                    tmp.writeText(json.encodeToString(serializer, _months.value))
                    if (!tmp.renameTo(file)) throw IOException("rename failed")
                } catch (e: IOException) {
                    _problem.value = StorageProblem.SaveFailed
                }
            }
            BudgetWidget.updateAll(context)
        }
    }

    /** Übernimmt nur Monate, die es noch nicht gibt; vorhandene bleiben unverändert. */
    fun importMonths(parsed: ParsedBackup): ImportResult {
        val existing = _months.value.map(Month::key).toSet()
        val new = parsed.months.distinctBy(Month::key).filter { it.key !in existing }
        if (new.isNotEmpty()) update { list -> list + new.filter { n -> list.none { it.key == n.key } } }
        return ImportResult(
            imported = new.size,
            skipped = parsed.months.size - new.size,
            invalid = parsed.invalid,
        )
    }

    fun dismissProblem() {
        _problem.value = null
    }

    private fun load(): List<Month> {
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString(serializer, file.readText()).sortedByDescending(Month::key)
        } catch (e: Exception) {
            // Unlesbare Datei sichern, damit sie beim nächsten Speichern nicht überschrieben wird.
            val backup = File(file.parentFile, "budget-unreadable-${System.currentTimeMillis()}.json")
            if (!file.renameTo(backup)) file.copyTo(backup, overwrite = true)
            _problem.value = StorageProblem.LoadFailed(backup.name)
            emptyList()
        }
    }

    companion object {
        @Volatile private var instance: BudgetRepository? = null

        fun get(context: Context): BudgetRepository =
            instance ?: synchronized(this) {
                instance ?: BudgetRepository(context.applicationContext).also { instance = it }
            }
    }
}
