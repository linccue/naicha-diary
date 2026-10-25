package com.naicha.diary.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.naicha.diary.R

class DiaryWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            // 这里先写死测试
            views.setTextViewText(R.id.tv_month_count, "8杯")
            views.setTextViewText(R.id.tv_date, "10月25日 喝过")
            views.setTextViewText(R.id.tv_drink_name, "珍珠奶茶")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
