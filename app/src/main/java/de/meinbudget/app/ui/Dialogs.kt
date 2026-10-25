package de.meinbudget.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.meinbudget.app.Entry
import de.meinbudget.app.R
import de.meinbudget.app.Month
import de.meinbudget.app.WallosResult
import de.meinbudget.app.formatMoney
import de.meinbudget.app.formatPlain
import de.meinbudget.app.parseCents
import kotlinx.coroutines.launch
import java.time.YearMonth

/** Dialog für die drei Basiswerte; beim Anlegen zusätzlich mit Monatsauswahl. */
@Composable
fun BaseValuesDialog(
    title: String,
    month: Month,
    allowMonthChange: Boolean,
    takenKeys: Set<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Month) -> Unit,
    /** Null = Wallos nicht aktiv; sonst wird ein Button zum Laden der Fixkosten angezeigt. */
    loadFromWallos: (suspend (year: Int, month: Int) -> WallosResult)? = null,
) {
    var yearMonth by remember { mutableStateOf(YearMonth.of(month.year, month.month)) }
    var opening by remember { mutableStateOf(formatPlain(month.openingCents)) }
    var income by remember { mutableStateOf(formatPlain(month.incomeCents)) }
    var fixed by remember { mutableStateOf(formatPlain(month.fixedCents)) }

    val openingCents = parseCents(opening)
    val incomeCents = parseCents(income)
    val fixedCents = parseCents(fixed)
    val key = Month.keyOf(yearMonth.year, yearMonth.monthValue)
    val monthTaken = allowMonthChange && key in takenKeys
    val valid = openingCents != null && incomeCents != null && fixedCents != null && !monthTaken
    val preview = month.copy(year = yearMonth.year, month = yearMonth.monthValue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (allowMonthChange) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { yearMonth = yearMonth.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_month))
                        }
                        Text(
                            preview.title,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { yearMonth = yearMonth.plusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_month))
                        }
                    }
                    if (monthTaken) {
                        Text(
                            stringResource(R.string.month_exists),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                MoneyField(stringResource(R.string.opening_balance), opening) { opening = it }
                MoneyField(stringResource(R.string.income), income) { income = it }
                MoneyField(stringResource(R.string.fixed_expenses), fixed) { fixed = it }
                if (loadFromWallos != null) {
                    WallosLoadButton(
                        yearMonth = yearMonth,
                        load = loadFromWallos,
                        onLoaded = { fixed = formatPlain(it) },
                    )
                }
                if (openingCents != null && incomeCents != null && fixedCents != null) {
                    Text(
                        stringResource(R.string.budget_result, formatMoney(openingCents + incomeCents - fixedCents)),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onConfirm(
                        preview.copy(
                            openingCents = openingCents!!,
                            incomeCents = incomeCents!!,
                            fixedCents = fixedCents!!,
                        )
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** "Lade aus Wallos" mit Ladeanzeige und Ergebnis- bzw. Fehlermeldung darunter. */
@Composable
private fun WallosLoadButton(
    yearMonth: YearMonth,
    load: suspend (year: Int, month: Int) -> WallosResult,
    onLoaded: (cents: Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var result by remember(yearMonth) { mutableStateOf<WallosResult?>(null) }

    OutlinedButton(
        onClick = {
            loading = true
            scope.launch {
                val r = load(yearMonth.year, yearMonth.monthValue)
                if (r is WallosResult.Success) onLoaded(r.cents)
                result = r
                loading = false
            }
        },
        enabled = !loading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Text(stringResource(R.string.wallos_load), Modifier.padding(start = 8.dp))
    }

    result?.let { r ->
        val isError = r !is WallosResult.Success
        Text(
            wallosResultText(r),
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Lesbare Meldung zu einem Wallos-Ergebnis (auch für "Verbindung testen" in den Einstellungen). */
@Composable
fun wallosResultText(result: WallosResult): String = when (result) {
    is WallosResult.Success -> {
        val loaded = stringResource(R.string.wallos_loaded, formatMoney(result.cents))
        if (result.currencyCode != null && result.currencyCode != "EUR") {
            loaded + " " + stringResource(R.string.wallos_currency_warning, result.currencyCode)
        } else {
            loaded
        }
    }
    WallosResult.NetworkError -> stringResource(R.string.wallos_error_network)
    is WallosResult.HttpError -> stringResource(R.string.wallos_error_http, result.code)
    WallosResult.InvalidResponse -> stringResource(R.string.wallos_error_invalid)
    is WallosResult.ApiError -> stringResource(
        R.string.wallos_error_api,
        result.message ?: stringResource(R.string.wallos_error_unknown),
    )
}

/** Bearbeiten einer bestehenden Buchung. */
@Composable
fun EntryDialog(
    entry: Entry,
    onDismiss: () -> Unit,
    onSave: (Entry) -> Unit,
) {
    var text by remember { mutableStateOf(entry.text) }
    var amount by remember { mutableStateOf(formatPlain(entry.amountCents)) }
    val cents = parseCents(amount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_entry)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.description)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                MoneyField(stringResource(R.string.amount_hint), amount) { amount = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = cents != null && cents != 0L,
                onClick = { onSave(entry.copy(text = text.trim(), amountCents = cents!!)) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ConfirmDialog(text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun MoneyField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        suffix = { Text("€") },
        singleLine = true,
        isError = parseCents(value) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
