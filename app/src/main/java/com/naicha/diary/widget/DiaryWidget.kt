package com.naicha.diary.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.naicha.diary.R
import com.naicha.diary.data.DrinkRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DiaryWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val repository = DrinkRepository(context)
        val records = repository.items.value

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val monthCount = records.count {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
        }
        val countText = monthCount.toString()

        val latest = records.maxByOrNull { it.timestamp }
        val dateText: String
        val nameText: String

        if (latest != null) {
            val dateStr = SimpleDateFormat("M月d日", Locale.CHINA).format(latest.timestamp)
            dateText = dateStr
            nameText = when {
                latest.name.isNotBlank() -> latest.name
                latest.brand.isNotBlank() -> latest.brand
                else -> "一杯饮品"
            }
        } else {
            dateText = "--"
            nameText = "暂无记录"
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setTextViewText(R.id.tv_month_count, countText)
            views.setTextViewText(R.id.tv_date, dateText)
            views.setTextViewText(R.id.tv_drink_name, nameText)

            // 获取启动意图
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                // 增加 SINGLE_TOP，防止 App 重复开启多个页面
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            
            if (launchIntent != null) {
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId, // 必须带 ID 区分不同组件
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                // 关键点：把点击事件同时绑定到 根布局 和 里面的每一个元素
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                views.setOnClickPendingIntent(R.id.tv_month_count, pendingIntent)
                views.setOnClickPendingIntent(R.id.tv_drink_name, pendingIntent)
                views.setOnClickPendingIntent(R.id.tv_date, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
