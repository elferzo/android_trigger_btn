package com.example.triggerbtn

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlin.concurrent.thread

class PlaudWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_BUTTON_CLICK   = "com.example.triggerbtn.PLAUD_CLICK"
        const val ACTION_MIDNIGHT_RESET = "com.example.triggerbtn.PLAUD_RESET"
        const val PREFS_NAME     = "WeeekPrefs"
        const val KEY_GREEN      = "plaud_green"
        const val KEY_LAST_CLICK = "plaud_last_click"
        const val WEBHOOK_URL    = "http://178.208.86.99:5000/run_plaud"
        const val DEBOUNCE_MS    = 5000L

        fun setColor(context: Context, green: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_GREEN, green).apply()
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(android.content.ComponentName(context, PlaudWidget::class.java))
            val color = if (green) Color.parseColor("#4CAF50") else Color.parseColor("#9E9E9E")
            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_plaud)
                views.setInt(R.id.btn_root_plaud, "setBackgroundColor", color)
                val intent = Intent(context, PlaudWidget::class.java).apply { action = ACTION_BUTTON_CLICK }
                val pi = PendingIntent.getBroadcast(context, id + 1000, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.btn_root_plaud, pi)
                manager.updateAppWidget(id, views)
            }
        }

        fun scheduleMidnightReset(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PlaudWidget::class.java).apply { action = ACTION_MIDNIGHT_RESET }
            val pi = PendingIntent.getBroadcast(context, 20, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            else am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isGreen = prefs.getBoolean(KEY_GREEN, false)
            val views = RemoteViews(context.packageName, R.layout.widget_plaud)
            views.setInt(R.id.btn_root_plaud, "setBackgroundColor",
                if (isGreen) Color.parseColor("#4CAF50") else Color.parseColor("#9E9E9E"))
            val intent = Intent(context, PlaudWidget::class.java).apply { action = ACTION_BUTTON_CLICK }
            val pi = PendingIntent.getBroadcast(context, id + 1000, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_root_plaud, pi)
            appWidgetManager.updateAppWidget(id, views)
        }
        scheduleMidnightReset(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_BUTTON_CLICK   -> handleButtonClick(context)
            ACTION_MIDNIGHT_RESET -> handleMidnightReset(context)
        }
    }

    private fun handleButtonClick(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_CLICK, 0L) < DEBOUNCE_MS) return
        prefs.edit().putLong(KEY_LAST_CLICK, now).apply()

        // Сразу зелёная
        Handler(Looper.getMainLooper()).post { setColor(context, true) }

        thread {
            try {
                val conn = URL(WEBHOOK_URL).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 10000
                conn.readTimeout = 60000
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) { }
            // После завершения — серая через main thread
            Handler(Looper.getMainLooper()).post { setColor(context, false) }
        }
    }

    private fun handleMidnightReset(context: Context) {
        setColor(context, false)
        scheduleMidnightReset(context)
    }
}
