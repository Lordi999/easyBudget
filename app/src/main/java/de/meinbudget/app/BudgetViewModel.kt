package de.meinbudget.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

sealed interface TransferResult {
    data class Exported(val count: Int) : TransferResult
    data class Imported(val result: ImportResult) : TransferResult
    data object WriteFailed : TransferResult
    /** Datei nicht lesbar oder größer als erlaubt */
    data object ReadFailed : TransferResult
    /** Datei lesbar, aber kein Export dieser App */
    data object InvalidFile : TransferResult
}

class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BudgetRepository.get(app)
    private val settings = SettingsRepository(app)
    val months: StateFlow<List<Month>> = repo.months
    val storageProblem: StateFlow<StorageProblem?> = repo.problem
    fun dismissStorageProblem() = repo.dismissProblem()
    val themeMode: StateFlow<ThemeMode> = settings.themeMode

    val language: StateFlow<AppLanguage> = settings.language

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)
    fun setLanguage(language: AppLanguage) = settings.setLanguage(language)

    val wallos: StateFlow<WallosConfig> = settings.wallos
    fun setWallos(config: WallosConfig) = settings.setWallos(config)

    /** Schreibt alle Monate in die vom Nutzer gewählte Datei. */
    suspend fun exportTo(uri: Uri): TransferResult = withContext(Dispatchers.IO) {
        val months = months.value
        val text = BudgetBackup.encode(months, LocalDate.now().toString())
        try {
            val resolver = getApplication<Application>().contentResolver
            // "wt" kürzt eine überschriebene Datei; nicht jeder Anbieter kennt den Modus.
            val stream = runCatching { resolver.openOutputStream(uri, "wt") }.getOrNull()
                ?: resolver.openOutputStream(uri)
                ?: return@withContext TransferResult.WriteFailed
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            TransferResult.Exported(months.size)
        } catch (e: Exception) {
            TransferResult.WriteFailed
        }
    }

    /** Liest eine Export-Datei; vorhandene Monate werden übersprungen. */
    suspend fun importFrom(uri: Uri): TransferResult = withContext(Dispatchers.IO) {
        val text = try {
            getApplication<Application>().contentResolver.openInputStream(uri)
                ?.let { readLimitedText(it, BudgetBackup.MAX_BYTES) }
        } catch (e: Exception) {
            null
        } ?: return@withContext TransferResult.ReadFailed
        val parsed = BudgetBackup.decode(text) ?: return@withContext TransferResult.InvalidFile
        TransferResult.Imported(repo.importMonths(parsed))
    }

    /**
     * Vorschlag für den nächsten Monat: Folgemonat des neuesten Monats,
     * Einkommen und Fixkosten übernommen, Anfangssaldo = Restbudget des Vormonats.
     */
    fun suggestNewMonth(): Month {
        val latest = months.value.maxByOrNull(Month::key)
            ?: return YearMonth.now().let { Month(it.year, it.monthValue, 0, 0, 0) }
        val next = YearMonth.of(latest.year, latest.month).plusMonths(1)
        return Month(
            year = next.year,
            month = next.monthValue,
            openingCents = latest.remainingCents,
            incomeCents = latest.incomeCents,
            fixedCents = latest.fixedCents,
        )
    }

    fun addMonth(month: Month) = repo.update { list ->
        if (list.any { it.key == month.key }) list else list + month
    }

    fun deleteMonth(key: Int) = repo.update { list -> list.filterNot { it.key == key } }

    fun updateBase(key: Int, opening: Long, income: Long, fixed: Long) = editMonth(key) {
        it.copy(openingCents = opening, incomeCents = income, fixedCents = fixed)
    }

    fun addEntry(key: Int, text: String, amountCents: Long) = editMonth(key) { m ->
        val id = (m.entries.maxOfOrNull(Entry::id) ?: 0) + 1
        m.copy(entries = m.entries + Entry(id, text, amountCents))
    }

    fun updateEntry(key: Int, entry: Entry) = editMonth(key) { m ->
        m.copy(entries = m.entries.map { if (it.id == entry.id) entry else it })
    }

    fun deleteEntry(key: Int, id: Long) = editMonth(key) { m ->
        m.copy(entries = m.entries.filterNot { it.id == id })
    }

    /** Stellt eine gelöschte Buchung an ihrer alten Position wieder her ("Rückgängig"). */
    fun restoreEntry(key: Int, entry: Entry) = editMonth(key) { m ->
        // Wurde die ID inzwischen neu vergeben, bekommt die Buchung eine neue (dann am Ende).
        val restored = if (m.entries.any { it.id == entry.id }) {
            entry.copy(id = m.entries.maxOf(Entry::id) + 1)
        } else {
            entry
        }
        m.copy(entries = (m.entries + restored).sortedBy(Entry::id))
    }

    private fun editMonth(key: Int, transform: (Month) -> Month) = repo.update { list ->
        list.map { if (it.key == key) transform(it) else it }
    }
}
