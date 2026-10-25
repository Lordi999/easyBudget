package de.meinbudget.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import de.meinbudget.app.AppLanguage
import de.meinbudget.app.R
import de.meinbudget.app.ThemeMode
import de.meinbudget.app.WallosClient
import de.meinbudget.app.WallosConfig
import de.meinbudget.app.WallosResult
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    wallos: WallosConfig,
    onWallosChange: (WallosConfig) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(stringResource(R.string.appearance))
            SettingDropdown(
                label = stringResource(R.string.theme),
                options = ThemeMode.entries,
                selected = themeMode,
                optionLabel = { stringResource(it.label) },
                onSelect = onThemeModeChange,
            )
            SettingDropdown(
                label = stringResource(R.string.language),
                options = AppLanguage.entries,
                selected = language,
                optionLabel = { it.nativeName ?: stringResource(R.string.language_system) },
                onSelect = onLanguageChange,
            )

            SectionHeader(stringResource(R.string.wallos_section))
            WallosSettings(wallos, onWallosChange)
        }
    }
}

@Composable
private fun WallosSettings(config: WallosConfig, onChange: (WallosConfig) -> Unit) {
    val scope = rememberCoroutineScope()
    var showKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<WallosResult?>(null) }

    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(
                value = config.enabled,
                onValueChange = { onChange(config.copy(enabled = it)) },
                role = Role.Switch,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.wallos_enabled), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.wallos_enabled_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = config.enabled, onCheckedChange = null)
    }

    if (!config.enabled) return

    OutlinedTextField(
        value = config.url,
        onValueChange = { onChange(config.copy(url = it.trim())); testResult = null },
        label = { Text(stringResource(R.string.wallos_url)) },
        placeholder = { Text("https://wallos.example.com") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        supportingText = if (config.url.startsWith("http://")) {
            { Text(stringResource(R.string.wallos_http_warning)) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = config.apiKey,
        onValueChange = { onChange(config.copy(apiKey = it.trim())); testResult = null },
        label = { Text(stringResource(R.string.wallos_api_key)) },
        singleLine = true,
        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        trailingIcon = {
            TextButton(onClick = { showKey = !showKey }) {
                Text(stringResource(if (showKey) R.string.hide else R.string.show))
            }
        },
        supportingText = { Text(stringResource(R.string.wallos_api_key_help)) },
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedButton(
        onClick = {
            testing = true
            scope.launch {
                val now = YearMonth.now()
                testResult = WallosClient.monthlyCost(config, now.year, now.monthValue)
                testing = false
            }
        },
        enabled = config.isUsable && !testing,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (testing) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(stringResource(R.string.wallos_test))
    }
    testResult?.let { result ->
        Text(
            wallosResultText(result),
            style = MaterialTheme.typography.bodySmall,
            color = if (result is WallosResult.Success) LocalAmountColors.current.positive
            else MaterialTheme.colorScheme.error,
        )
    }
}

/** Auswahlfeld mit aufklappender Liste – wiederverwendbar für weitere Einstellungen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp),
    )
}
