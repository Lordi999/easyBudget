package de.meinbudget.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import java.time.YearMonth

/** Homescreen-Widget: verfügbares Budget des aktuellen Monats. */
class BudgetWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        update(context, manager, widgetIds)
    }

    companion object {
        /** Intent-Extra: Schlüssel des Monats, der beim Öffnen der App angezeigt wird */
        const val EXTRA_OPEN_MONTH = "de.meinbudget.app.OPEN_MONTH"

        /** Aktualisiert alle Widgets, z. B. nach jeder Änderung der Daten. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, BudgetWidget::class.java))
            if (ids.isNotEmpty()) update(context, manager, ids)
        }

        private fun update(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
            // Texte in der in der App gewählten Sprache, nicht nur in der Systemsprache
            val localized = SettingsRepository.localizedContext(context)
            val now = YearMonth.now()
            val key = Month.keyOf(now.year, now.monthValue)
            val month = BudgetRepository.get(context).months.value.find { it.key == key }

            val views = RemoteViews(context.packageName, R.layout.widget_budget)
            if (month != null) {
                views.setTextViewText(R.id.widget_title, month.title)
                views.setTextViewText(R.id.widget_amount, formatMoney(month.remainingCents))
                views.setTextViewText(R.id.widget_label, localized.getString(R.string.available))
                setAmountColor(
                    context, views,
                    if (month.remainingCents < 0) R.color.amount_negative else R.color.amount_positive,
                )
            } else {
                val title = Month(now.year, now.monthValue, 0, 0, 0).title
                views.setTextViewText(R.id.widget_title, localized.getString(R.string.app_name))
                views.setTextViewText(R.id.widget_amount, localized.getString(R.string.widget_no_month, title))
                views.setTextViewText(R.id.widget_label, localized.getString(R.string.widget_tap_to_create))
                setAmountColor(context, views, R.color.widget_text)
            }
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context, month?.key))
            manager.updateAppWidget(widgetIds, views)
        }

        /** Farbe als Ressource, damit der Launcher bei Hell/Dunkel-Wechsel selbst umschaltet (ab Android 12). */
        private fun setAmountColor(context: Context, views: RemoteViews, colorRes: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setColorStateList(R.id.widget_amount, "setTextColor", colorRes)
            } else {
                views.setTextColor(R.id.widget_amount, context.getColor(colorRes))
            }
        }

        private fun openAppIntent(context: Context, monthKey: Int?): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                if (monthKey != null) putExtra(EXTRA_OPEN_MONTH, monthKey)
            }
            return PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
