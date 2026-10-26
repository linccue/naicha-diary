package com.naicha.diary.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.naicha.diary.R
import java.util.Calendar

class AbstinenceWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences("abstinence_prefs", Context.MODE_PRIVATE)
        val isTracking = prefs.getBoolean("is_tracking", false)
        val targetDays = prefs.getInt("target_days", 7)
        val startDate = prefs.getLong("start_date", 0L)
        val drankDaysStr = prefs.getString("drank_days", "") ?: ""
        
        val drankDays = if (drankDaysStr.isEmpty()) emptySet() else drankDaysStr.split(",").map { it.toInt() }.toSet()

        // 计算天数
        val todayDiff = if (startDate == 0L) 0 else {
            val cal1 = Calendar.getInstance().apply { timeInMillis = startDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            val cal2 = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            ((cal2.timeInMillis - cal1.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        }
        
        val isTodayDrank = drankDays.contains(todayDiff)
        val validStreak = if (isTodayDrank) 0 else todayDiff - drankDays.count { it < todayDiff }

        val views = RemoteViews(context.packageName, R.layout.widget_abstinence_layout)

        if (!isTracking) {
            views.setTextViewText(R.id.tv_abs_title, "不喝打卡")
            views.setTextViewText(R.id.tv_abs_days, "未开始")
            views.setTextViewText(R.id.tv_abs_status, "去 App 内设置目标")
            views.setTextViewText(R.id.tv_abs_sub, "")
        } else {
            views.setTextViewText(R.id.tv_abs_title, if (isTodayDrank) "今日破戒" else "不喝打卡")
            views.setTextViewText(R.id.tv_abs_days, "已连续 $validStreak 天")
            views.setTextViewText(R.id.tv_abs_status, "进度 ${todayDiff + 1}/$targetDays")
            views.setTextViewText(R.id.tv_abs_sub, "还差 ${targetDays - todayDiff - 1} 天")
        }

        // 点击打开 App
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launchIntent?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else null

        for (appWidgetId in appWidgetIds) {
            if (pendingIntent != null) {
                views.setOnClickPendingIntent(R.id.widget_abs_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
