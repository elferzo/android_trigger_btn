package com.example.triggerbtn

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlin.concurrent.thread

class WeeekWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_BUTTON_CLICK   = "com.example.triggerbtn.BUTTON_CLICK"
        const val ACTION_MIDNIGHT_RESET = "com.example.triggerbtn.MIDNIGHT_RESET"
        const val PREFS_NAME = "WeeekPrefs"
        const val KEY_GREEN  = "is_green"
        const val WEBHOOK_URL = "http://178.208.86.99:5000/run"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isGreen = prefs.getBoolean(KEY_GREEN, false)

            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val color = if (isGreen) Color.parseColor("#4CAF50") else Color.parseColor("#9E9E9E")
            views.setInt(R.id.btn_root, "setBackgroundColor", color)

            // уникальный requestCode per widget id
            val intent = Intent(context, WeeekWidget::class.java).apply {
                action = ACTION_BUTTON_CLICK
            }
            val pi = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_root, pi)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, WeeekWidget::class.java)
            )
            ids.forEach { updateWidget(context, manager, it) }
        }

        fun scheduleMidnightReset(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WeeekWidget::class.java).apply {
                action = ACTION_MIDNIGHT_RESET
            }
            val pi = PendingIntent.getBroadcast(
                context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
        scheduleMidnightReset(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_BUTTON_CLICK  -> handleButtonClick(context)
            ACTION_MIDNIGHT_RESET -> handleMidnightReset(context)
        }
    }

    private fun handleButtonClick(context: Context) {
        // Сразу красим серым → "pending" пока ждём ответ
        thread {
            try {
                val url = URL(WEBHOOK_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 10000
                conn.readTimeout = 30000
                conn.doOutput = false
                val code = conn.responseCode
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_GREEN, code == 200).apply()
                conn.disconnect()
            } catch (e: Exception) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_GREEN, false).apply()
            }
            updateAllWidgets(context)
        }
    }

    private fun handleMidnightReset(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GREEN, false).apply()
        updateAllWidgets(context)
        scheduleMidnightReset(context)
    }
}
