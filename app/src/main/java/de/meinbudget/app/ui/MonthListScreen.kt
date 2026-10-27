package de.meinbudget.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.meinbudget.app.Month
import de.meinbudget.app.R
import de.meinbudget.app.WallosResult
import de.meinbudget.app.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthListScreen(
    months: List<Month>,
    suggestNewMonth: () -> Month,
    onCreate: (Month) -> Unit,
    onOpen: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    loadFromWallos: (suspend (year: Int, month: Int) -> WallosResult)?,
) {
    var newMonth by remember { mutableStateOf<Month?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { newMonth = suggestNewMonth() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_month))
            }
        },
    ) { padding ->
        if (months.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.no_months),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(months, key = { it.key }) { month ->
                    Card(onClick = { onOpen(month.key) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(month.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    pluralStringResource(
                                        R.plurals.entry_count,
                                        month.entries.size,
                                        month.entries.size,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                formatMoney(month.remainingCents),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = amountColor(month.remainingCents),
                            )
                        }
                    }
                }
            }
        }
    }

    newMonth?.let { suggestion ->
        BaseValuesDialog(
            title = stringResource(R.string.new_month),
            month = suggestion,
            allowMonthChange = true,
            takenKeys = months.map { it.key }.toSet(),
            onDismiss = { newMonth = null },
            onConfirm = { onCreate(it); newMonth = null },
            loadFromWallos = loadFromWallos,
        )
    }
}
