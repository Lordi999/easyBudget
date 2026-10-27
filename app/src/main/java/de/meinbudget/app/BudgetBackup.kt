package de.meinbudget.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Aufbau der Export-Datei. Enthält nur Budgetdaten, keine Einstellungen und keinen API-Key. */
@Serializable
data class BudgetExport(
    val format: String = BudgetBackup.FORMAT,
    val version: Int = 1,
    val exportedAt: String,
    val months: List<Month>,
)

/** Ergebnis einer Prüfung: gültige Monate und Anzahl verworfener (ungültiger) Monate. */
data class ParsedBackup(val months: List<Month>, val invalid: Int)

object BudgetBackup {
    const val FORMAT = "easybudget-export"

    /** Größere Dateien sind sicher kein Export dieser App. */
    const val MAX_BYTES = 10 * 1024 * 1024

    private const val MAX_TEXT_LENGTH = 500
    private val maxCents = MAX_AMOUNT.movePointRight(2).toLong()

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    fun encode(months: List<Month>, exportedAt: String): String =
        json.encodeToString(BudgetExport.serializer(), BudgetExport(exportedAt = exportedAt, months = months))

    /**
     * Liest eine Export-Datei – oder auch direkt eine budget.json (reine Liste).
     * Gibt null zurück, wenn die Datei kein lesbares Format hat.
     */
    fun decode(text: String): ParsedBackup? {
        val months = try {
            when (val root = json.parseToJsonElement(text)) {
                is JsonObject -> {
                    if ((root["format"] as? JsonPrimitive)?.contentOrNull != FORMAT) return null
                    json.decodeFromJsonElement(BudgetExport.serializer(), root).months
                }
                is JsonArray -> json.decodeFromJsonElement(ListSerializer(Month.serializer()), root)
                else -> return null
            }
        } catch (e: Exception) {
            return null
        }
        return sanitize(months)
    }

    /** Verwirft ungültige Monate, kürzt Texte und vergibt doppelte Buchungs-IDs neu. */
    internal fun sanitize(months: List<Month>): ParsedBackup {
        val valid = mutableListOf<Month>()
        var invalid = 0
        for (month in months) {
            val ok = month.month in 1..12 && month.year in 1900..2200 &&
                listOf(month.openingCents, month.incomeCents, month.fixedCents).all { kotlin.math.abs(it) <= maxCents } &&
                month.entries.all { kotlin.math.abs(it.amountCents) <= maxCents }
            if (!ok) {
                invalid++
                continue
            }
            val seenIds = mutableSetOf<Long>()
            var nextId = (month.entries.maxOfOrNull(Entry::id) ?: 0) + 1
            val entries = month.entries.map { entry ->
                val id = if (seenIds.add(entry.id)) entry.id else nextId++.also { seenIds.add(it) }
                entry.copy(id = id, text = entry.text.take(MAX_TEXT_LENGTH))
            }
            valid += month.copy(entries = entries)
        }
        return ParsedBackup(valid, invalid)
    }
}
