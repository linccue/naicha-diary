package com.naicha.diary.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.naicha.diary.R

// 注意这里改成了 AppWidgetProvider
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
        // 绑定我们刚才画好的 XML
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // 这里先用假数据测试，确保它能在红米桌面上显示出来
        views.setTextViewText(R.id.tv_month_count, "8杯")
        views.setTextViewText(R.id.tv_date, "10月25日 喝过")
        views.setTextViewText(R.id.tv_drink_name, "珍珠奶茶")

        // 通知桌面更新
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
