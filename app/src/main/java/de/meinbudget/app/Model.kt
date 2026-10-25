package de.meinbudget.app

import kotlinx.serialization.Serializable
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Eine Buchung: negativ = Ausgabe, positiv = Einnahme. Beträge immer in Cent. */
@Serializable
data class Entry(
    val id: Long,
    val text: String,
    val amountCents: Long,
)

@Serializable
data class Month(
    val year: Int,
    val month: Int,
    val openingCents: Long,
    val incomeCents: Long,
    val fixedCents: Long,
    val entries: List<Entry> = emptyList(),
) {
    /** Eindeutiger, sortierbarer Schlüssel des Monats. */
    val key: Int get() = year * 12 + (month - 1)

    val title: String
        get() = YearMonth.of(year, month).month
            .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()) + " $year"

    /** Anfangssaldo + Einkommen − fixe Ausgaben */
    val budgetCents: Long get() = openingCents + incomeCents - fixedCents

    val spentCents: Long get() = entries.filter { it.amountCents < 0 }.sumOf { it.amountCents }
    val earnedCents: Long get() = entries.filter { it.amountCents > 0 }.sumOf { it.amountCents }

    /** Budget nach allen Buchungen */
    val remainingCents: Long get() = budgetCents + entries.sumOf { it.amountCents }

    companion object {
        fun keyOf(year: Int, month: Int) = year * 12 + (month - 1)
    }
}
