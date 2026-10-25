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

        // 1. 计算本月杯数
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val monthCount = records.count {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
        }
        val countText = monthCount.toString()

        // 2. 获取最近一杯
        val latest = records.maxByOrNull { it.timestamp }
        val dateText: String
        val nameText: String

        if (latest != null) {
            // 如果像样例一样不加“喝过”，就把 " 喝过" 删掉即可，这里保留了“喝过”
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

        // 3. 最稳妥的跳转方案：自动去抓取 App 的桌面启动入口
        // 这样不管你的启动页叫 MainActivity 还是别的，都能准确打开！
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launchIntent?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        // 4. 更新到桌面小部件
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setTextViewText(R.id.tv_month_count, countText)
            views.setTextViewText(R.id.tv_date, dateText)
            views.setTextViewText(R.id.tv_drink_name, nameText)
            
            // 绑定点击事件
            if (pendingIntent != null) {
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
