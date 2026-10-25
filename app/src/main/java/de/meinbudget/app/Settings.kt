package de.meinbudget.app

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class ThemeMode(@StringRes val label: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}

/** [tag] null = Systemsprache. Sprachnamen stehen bewusst in ihrer eigenen Sprache. */
enum class AppLanguage(val tag: String?, val nativeName: String?) {
    SYSTEM(null, null),
    GERMAN("de", "Deutsch"),
    ENGLISH("en", "English"),
}

/** Zugangsdaten für einen (selbst gehosteten) Wallos-Server. */
data class WallosConfig(val enabled: Boolean, val url: String, val apiKey: String) {
    /** Aktiviert und vollständig ausgefüllt */
    val isUsable: Boolean get() = enabled && url.isNotBlank() && apiKey.isNotBlank()
}

/** App-Einstellungen, gespeichert in den SharedPreferences. Weitere Einstellungen kommen hier dazu. */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readEnum(KEY_THEME, ThemeMode.SYSTEM))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(readEnum(KEY_LANGUAGE, AppLanguage.SYSTEM))
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    private val _wallos = MutableStateFlow(
        WallosConfig(
            enabled = prefs.getBoolean(KEY_WALLOS_ENABLED, false),
            url = prefs.getString(KEY_WALLOS_URL, "").orEmpty(),
            apiKey = prefs.getString(KEY_WALLOS_API_KEY, "").orEmpty(),
        )
    )
    val wallos: StateFlow<WallosConfig> = _wallos.asStateFlow()

    fun setWallos(config: WallosConfig) {
        _wallos.value = config
        prefs.edit()
            .putBoolean(KEY_WALLOS_ENABLED, config.enabled)
            .putString(KEY_WALLOS_URL, config.url)
            .putString(KEY_WALLOS_API_KEY, config.apiKey)
            .apply()
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T =
        prefs.getString(key, null)
            ?.let { name -> enumValues<T>().find { it.name == name } }
            ?: default

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_WALLOS_ENABLED = "wallos_enabled"
        private const val KEY_WALLOS_URL = "wallos_url"
        private const val KEY_WALLOS_API_KEY = "wallos_api_key"

        /**
         * Liefert einen Context mit der gewählten App-Sprache.
         * Wird in MainActivity.attachBaseContext verwendet; ein Sprachwechsel startet die Activity neu.
         */
        fun localizedContext(base: Context): Context {
            val language = SettingsRepository(base).language.value
            val locale = language.tag?.let(Locale::forLanguageTag)
                ?: Resources.getSystem().configuration.locales[0]
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            return base.createConfigurationContext(config)
        }
    }
}
