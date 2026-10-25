package de.meinbudget.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import de.meinbudget.app.ui.BudgetTheme
import de.meinbudget.app.ui.MonthListScreen
import de.meinbudget.app.ui.MonthScreen
import de.meinbudget.app.ui.SettingsScreen
import de.meinbudget.app.ui.isDarkTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SettingsRepository.localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: BudgetViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsState()
            val dark = isDarkTheme(themeMode)

            // Statusleisten-Symbole passend zum gewählten Modus (nicht nur zum System) einfärben.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            BudgetTheme(dark = dark) {
                BudgetApp(vm, onLanguageChange = { language ->
                    if (language != vm.language.value) {
                        vm.setLanguage(language)
                        recreate()
                    }
                })
            }
        }
    }
}

@Composable
fun BudgetApp(vm: BudgetViewModel, onLanguageChange: (AppLanguage) -> Unit) {
    val months by vm.months.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val language by vm.language.collectAsState()
    val wallos by vm.wallos.collectAsState()
    val storageProblem by vm.storageProblem.collectAsState()
    var openKey by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val open = months.find { it.key == openKey }

    when {
        showSettings -> {
            BackHandler { showSettings = false }
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = vm::setThemeMode,
                language = language,
                onLanguageChange = onLanguageChange,
                wallos = wallos,
                onWallosChange = vm::setWallos,
                onBack = { showSettings = false },
            )
        }
        open != null -> {
            BackHandler { openKey = null }
            MonthScreen(
                month = open,
                onBack = { openKey = null },
                onEditBase = { opening, income, fixed -> vm.updateBase(open.key, opening, income, fixed) },
                onDelete = { vm.deleteMonth(open.key); openKey = null },
                onAddEntry = { text, cents -> vm.addEntry(open.key, text, cents) },
                onUpdateEntry = { vm.updateEntry(open.key, it) },
                onDeleteEntry = { vm.deleteEntry(open.key, it) },
                onRestoreEntry = { vm.restoreEntry(open.key, it) },
            )
        }
        else -> {
            MonthListScreen(
                months = months,
                suggestNewMonth = vm::suggestNewMonth,
                onCreate = { vm.addMonth(it); openKey = it.key },
                onOpen = { openKey = it },
                onOpenSettings = { showSettings = true },
                loadFromWallos = if (wallos.isUsable) {
                    { year, month -> WallosClient.monthlyCost(wallos, year, month) }
                } else {
                    null
                },
            )
        }
    }

    storageProblem?.let { problem ->
        AlertDialog(
            onDismissRequest = vm::dismissStorageProblem,
            title = { Text(stringResource(R.string.storage_problem_title)) },
            text = {
                Text(
                    when (problem) {
                        is StorageProblem.LoadFailed ->
                            stringResource(R.string.storage_load_failed, problem.backupName)
                        StorageProblem.SaveFailed -> stringResource(R.string.storage_save_failed)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = vm::dismissStorageProblem) { Text(stringResource(R.string.ok)) }
            },
        )
    }
}
