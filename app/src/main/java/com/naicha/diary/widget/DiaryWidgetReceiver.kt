package com.naicha.diary.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class DiaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DiaryWidget()
    // 强制使用 RemoteViews 后端以兼容 HyperOS
    override fun getAppWidgetOptions(): Bundle {
        return Bundle().apply {
            putBoolean("GLANCE_OPTION_APPWIDGET_FORCE_BACKEND", true)
        }
    }
}
