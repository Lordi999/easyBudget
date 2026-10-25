package de.meinbudget.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.meinbudget.app.Entry
import de.meinbudget.app.Month
import de.meinbudget.app.R
import de.meinbudget.app.formatMoney
import de.meinbudget.app.formatSigned
import de.meinbudget.app.parseCents
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    month: Month,
    onBack: () -> Unit,
    onEditBase: (opening: Long, income: Long, fixed: Long) -> Unit,
    onDelete: () -> Unit,
    onAddEntry: (text: String, amountCents: Long) -> Unit,
    onUpdateEntry: (Entry) -> Unit,
    onDeleteEntry: (id: Long) -> Unit,
    onRestoreEntry: (Entry) -> Unit,
) {
    var showBaseDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var revealedId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun deleteWithUndo(entry: Entry) {
        onDeleteEntry(entry.id)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = context.getString(
                    R.string.entry_deleted,
                    entry.text.ifBlank { context.getString(R.string.entry_fallback) },
                ),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onRestoreEntry(entry)
        }
    }

    // Nach einer neuen Buchung automatisch ans Listenende scrollen.
    LaunchedEffect(month.entries.size) {
        if (month.entries.isNotEmpty()) listState.animateScrollToItem(month.entries.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(month.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showBaseDialog = true }) {
                        Icon(Icons.Default.Edit, stringResource(R.string.edit_base_values))
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.more))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_month)) },
                                onClick = { showMenu = false; confirmDelete = true },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = { EntryInputBar(onAdd = onAddEntry) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            BudgetHeader(month, onClick = { showBaseDialog = true })

            if (month.entries.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_entries),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    items(month.entries, key = { it.id }) { entry ->
                        SwipeableEntryRow(
                            entry = entry,
                            revealed = revealedId == entry.id,
                            onRevealedChange = { open ->
                                if (open) revealedId = entry.id
                                else if (revealedId == entry.id) revealedId = null
                            },
                            onDelete = { deleteWithUndo(entry) },
                            onEdit = { editing = entry },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showBaseDialog) {
        BaseValuesDialog(
            title = stringResource(R.string.base_values_title, month.title),
            month = month,
            allowMonthChange = false,
            takenKeys = emptySet(),
            onDismiss = { showBaseDialog = false },
            onConfirm = {
                onEditBase(it.openingCents, it.incomeCents, it.fixedCents)
                showBaseDialog = false
            },
        )
    }
    editing?.let { entry ->
        EntryDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { onUpdateEntry(it); editing = null },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            text = stringResource(R.string.delete_month_confirm, month.title),
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; onDelete() },
        )
    }
}

/** Großes Restbudget oben, darunter die Rechnung. Antippen öffnet die Basiswerte. */
@Composable
private fun BudgetHeader(month: Month, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.available), style = MaterialTheme.typography.labelLarge)
            Text(
                formatMoney(month.remainingCents),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (month.remainingCents < 0) LocalAmountColors.current.negative
                else MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.padding(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                HeaderValue(stringResource(R.string.budget), formatMoney(month.budgetCents))
                HeaderValue(stringResource(R.string.expenses), formatMoney(month.spentCents))
                HeaderValue(stringResource(R.string.earnings), formatSigned(month.earnedCents))
            }
            Spacer(Modifier.padding(2.dp))
            Text(
                stringResource(
                    R.string.calculation,
                    formatMoney(month.openingCents),
                    formatMoney(month.incomeCents),
                    formatMoney(month.fixedCents),
                ),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun HeaderValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Eingabezeile, die immer unten (über der Tastatur) sichtbar bleibt.
 * Der ±-Knopf wechselt zwischen Ausgabe und Einnahme; ein getipptes "+" oder "-" tut dasselbe.
 */
@Composable
private fun EntryInputBar(onAdd: (String, Long) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var isIncome by rememberSaveable { mutableStateOf(false) }
    val textFocus = remember { FocusRequester() }

    val cents = parseCents(amount)?.let { abs(it) }
    val canSubmit = cents != null && cents > 0

    fun submit() {
        if (!canSubmit) return
        onAdd(text.trim(), if (isIncome) cents!! else -cents!!)
        text = ""
        amount = ""
        isIncome = false
        textFocus.requestFocus()
    }

    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.description)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.weight(1f).focusRequester(textFocus),
            )
            FilledTonalIconButton(onClick = { isIncome = !isIncome }) {
                Text(
                    if (isIncome) "+" else "−",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) LocalAmountColors.current.positive
                    else LocalAmountColors.current.negative,
                )
            }
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    when {
                        it.startsWith("+") -> { isIncome = true; amount = it.drop(1) }
                        it.startsWith("-") -> { isIncome = false; amount = it.drop(1) }
                        else -> amount = it
                    }
                },
                placeholder = { Text("0,00") },
                suffix = { Text("€") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.width(110.dp),
            )
            IconButton(onClick = ::submit, enabled = canSubmit) {
                Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.add_entry))
            }
        }
    }
}
