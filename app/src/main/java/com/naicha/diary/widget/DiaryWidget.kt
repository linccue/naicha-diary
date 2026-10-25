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
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // 绑定我们画好的 XML 布局
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // 先写死假数据，确保能显示出来
        views.setTextViewText(R.id.tv_month_count, "8杯")
        views.setTextViewText(R.id.tv_date, "10月25日 喝过")
        views.setTextViewText(R.id.tv_drink_name, "珍珠奶茶")

        // 通知桌面刷新
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
