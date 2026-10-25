package de.meinbudget.app

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val currency: NumberFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY)

/** 123456 -> "1.234,56 €" */
fun formatMoney(cents: Long): String = currency.format(cents / 100.0)

/** Wie [formatMoney], aber mit "+" vor positiven Beträgen. */
fun formatSigned(cents: Long): String = (if (cents > 0) "+" else "") + formatMoney(cents)

/** 123456 -> "1234,56" (zum Vorbelegen von Eingabefeldern) */
fun formatPlain(cents: Long): String = String.format(Locale.GERMANY, "%.2f", cents / 100.0)

/** Höchster erlaubter Betrag (in €) – schützt vor Überlauf beim Rechnen in Cent. */
val MAX_AMOUNT: BigDecimal = BigDecimal(1_000_000_000)

private val NUMBER_CHARS = Regex("[0-9.,]+")

/**
 * Liest Eingaben wie "45", "45,5", "-12,99", "+200", "1.234,56 €".
 * Gibt null zurück, wenn die Eingabe keine Zahl ist oder über [MAX_AMOUNT] liegt.
 */
fun parseCents(input: String): Long? {
    var s = input.trim().replace("€", "").replace(" ", "")
    if (s.isEmpty()) return null
    val negative = s.startsWith("-")
    s = s.removePrefix("-").removePrefix("+")
    // Nur Ziffern und Trenner – keine Schreibweisen wie "1e5".
    if (!NUMBER_CHARS.matches(s)) return null
    // Mit Komma: Punkte sind Tausendertrenner. Ohne Komma: Punkt ist Dezimaltrenner.
    s = if (s.contains(',')) s.replace(".", "").replace(',', '.') else s
    val value = s.toBigDecimalOrNull()?.takeIf { it <= MAX_AMOUNT } ?: return null
    val cents = value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    return if (negative) -cents else cents
}
