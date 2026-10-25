package de.meinbudget.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import de.meinbudget.app.ThemeMode

/** Farben für Beträge. Eigene Werte statt colorScheme.error, das im Dunkelmodus blass-rosa ist. */
@Immutable
data class AmountColors(val positive: Color, val negative: Color)

private val LightAmountColors = AmountColors(
    positive = Color(0xFF2E7D32),
    negative = Color(0xFFC62828),
)
private val DarkAmountColors = AmountColors(
    positive = Color(0xFF5CD67A),
    negative = Color(0xFFFF6B6B),
)

/** Kräftiges Rot für die Löschen-Fläche, in beiden Modi gut lesbar mit weißer Schrift. */
val DeleteRed = Color(0xFFD32F2F)

val LocalAmountColors = staticCompositionLocalOf { LightAmountColors }

@Composable
fun amountColor(cents: Long): Color = when {
    cents < 0 -> LocalAmountColors.current.negative
    cents > 0 -> LocalAmountColors.current.positive
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun BudgetTheme(dark: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    CompositionLocalProvider(LocalAmountColors provides if (dark) DarkAmountColors else LightAmountColors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
