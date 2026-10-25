package com.naicha.diary.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
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
        // 获取真实数据源
        val repository = DrinkRepository(context)
        val records = repository.items.value

        // 1. 计算本月喝了几杯
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val monthCount = records.count {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
        }
        val countText = "${monthCount}杯"

        // 2. 获取最近一杯
        val latest = records.maxByOrNull { it.timestamp }
        val dateText: String
        val nameText: String

        if (latest != null) {
            val dateStr = SimpleDateFormat("M月d日", Locale.CHINA).format(latest.timestamp)
            dateText = "${dateStr} 喝过"
            // 如果没写饮品名，就显示品牌名
            nameText = when {
                latest.name.isNotBlank() -> latest.name
                latest.brand.isNotBlank() -> latest.brand
                else -> "一杯饮品"
            }
        } else {
            dateText = "--"
            nameText = "暂无记录"
        }

        // 3. 更新到桌面小部件
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setTextViewText(R.id.tv_month_count, countText)
            views.setTextViewText(R.id.tv_date, dateText)
            views.setTextViewText(R.id.tv_drink_name, nameText)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
