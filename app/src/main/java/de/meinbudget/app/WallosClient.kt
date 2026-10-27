package de.meinbudget.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed interface WallosResult {
    data class Success(val cents: Long, val currencyCode: String?) : WallosResult
    /** Server nicht erreichbar (falsche URL, kein Netz, Timeout) */
    data object NetworkError : WallosResult
    data class HttpError(val code: Int) : WallosResult
    /** Server leitet weiter (z. B. http → https); wir folgen nicht, damit der API-Key nicht woanders landet */
    data object Redirect : WallosResult
    /** Antwort ist kein Wallos-JSON, z. B. weil die URL auf eine andere Seite zeigt */
    data object InvalidResponse : WallosResult
    /** Wallos meldet einen Fehler, z. B. "Invalid API key" */
    data class ApiError(val message: String?) : WallosResult
}

/** Zugriff auf die Wallos-API: https://api.wallosapp.com */
object WallosClient {
    private val json = Json { ignoreUnknownKeys = true }

    /** Summe der Abos für den Monat (1–12) laut /api/subscriptions/get_monthly_cost.php */
    suspend fun monthlyCost(config: WallosConfig, year: Int, month: Int): WallosResult =
        withContext(Dispatchers.IO) {
            val connection = try {
                val key = URLEncoder.encode(config.apiKey.trim(), "UTF-8")
                URL("${baseUrl(config.url)}/api/subscriptions/get_monthly_cost.php?apiKey=$key&month=$month&year=$year")
                    .openConnection() as HttpURLConnection
            } catch (e: Exception) {
                return@withContext WallosResult.NetworkError
            }

            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                // Die URL enthält den API-Key – nicht automatisch zu einer anderen Adresse weiterreichen.
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")

                val code = connection.responseCode
                if (code in 300..399) return@withContext WallosResult.Redirect
                if (code !in 200..299) return@withContext WallosResult.HttpError(code)
                val body = readLimitedText(connection.inputStream, MAX_RESPONSE_BYTES)
                    ?: return@withContext WallosResult.InvalidResponse
                parse(body)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                WallosResult.NetworkError
            } catch (e: Exception) {
                // Unerwartete Fehler (z. B. ungültige Adresse) sollen die App nicht abstürzen lassen.
                WallosResult.NetworkError
            } finally {
                connection.disconnect()
            }
        }

    /** Größere Antworten sind keine Wallos-Antwort. */
    private const val MAX_RESPONSE_BYTES = 1024 * 1024

    private fun parse(body: String): WallosResult {
        val obj: JsonObject = runCatching { json.parseToJsonElement(body).jsonObject }
            .getOrElse { return WallosResult.InvalidResponse }

        val success = obj["success"]?.jsonPrimitive?.booleanOrNull
            ?: return WallosResult.InvalidResponse
        if (!success) return WallosResult.ApiError(obj["title"]?.jsonPrimitive?.contentOrNull)

        val cost = obj["monthly_cost"]?.jsonPrimitive?.contentOrNull?.let(::parseAmount)
            ?.takeIf { it.abs() <= MAX_AMOUNT }
            ?: return WallosResult.InvalidResponse
        return WallosResult.Success(
            cents = cost.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact(),
            currencyCode = obj["currency_code"]?.jsonPrimitive?.contentOrNull,
        )
    }

    /**
     * Laut Doku ist monthly_cost eine Zahl, Wallos liefert aber z. B. "2,045.32" als Text.
     * Akzeptiert beides: Der letzte Trenner vor 1–2 Endziffern ist das Dezimalzeichen, alle anderen Trenner fallen weg.
     */
    internal fun parseAmount(raw: String): BigDecimal? {
        val s = raw.trim()
        if (!Regex("-?[0-9.,]+").matches(s)) return null
        val decimalSep = Regex("[.,](\\d{1,2})$").find(s)?.range?.first
        val normalized = s.filterIndexed { i, c -> c.isDigit() || c == '-' || i == decimalSep }
            .replace(',', '.')
        return normalized.toBigDecimalOrNull()
    }

    /** "wallos.example.com/" → "https://wallos.example.com" */
    private fun baseUrl(url: String): String {
        val trimmed = url.trim().trimEnd('/')
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    }
}
