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

/** Speichert alle Monate als JSON-Datei im privaten App-Speicher. */
class BudgetRepository(context: Context) {
    private val file = File(context.filesDir, "budget.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()
    private val serializer = ListSerializer(Month.serializer())

    private val _months = MutableStateFlow(load())
    val months: StateFlow<List<Month>> = _months.asStateFlow()

    fun update(transform: (List<Month>) -> List<Month>) {
        _months.update { transform(it).sortedByDescending(Month::key) }
        scope.launch {
            writeLock.withLock {
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(json.encodeToString(serializer, _months.value))
                tmp.renameTo(file)
            }
        }
    }

    private fun load(): List<Month> =
        if (file.exists()) {
            runCatching { json.decodeFromString(serializer, file.readText()) }
                .getOrDefault(emptyList())
                .sortedByDescending(Month::key)
        } else {
            emptyList()
        }
}
