package de.meinbudget.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow
import java.time.YearMonth

class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BudgetRepository(app)
    private val settings = SettingsRepository(app)
    val months: StateFlow<List<Month>> = repo.months
    val themeMode: StateFlow<ThemeMode> = settings.themeMode

    val language: StateFlow<AppLanguage> = settings.language

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)
    fun setLanguage(language: AppLanguage) = settings.setLanguage(language)

    val wallos: StateFlow<WallosConfig> = settings.wallos
    fun setWallos(config: WallosConfig) = settings.setWallos(config)

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
