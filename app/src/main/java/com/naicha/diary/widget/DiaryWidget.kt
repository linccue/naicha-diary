package com.naicha.diary.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class DiaryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 暂时使用假数据，确保能编译通过
        val monthCount = 8
        val dateStr = "10月25日"
        val drinkName = "珍珠奶茶"

        provideContent {
            WidgetContent(monthCount, dateStr, drinkName)
        }
    }
}

@Composable
private fun WidgetContent(monthCount: Int, dateStr: String, drinkName: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(android.graphics.Color.WHITE)
            .padding(14.dp)
            .appWidgetBackground(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：约 3 份比例（定宽约 80dp）
        Column(
            modifier = GlanceModifier.width(80.dp).padding(end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "本月",
                style = TextStyle(color = ColorProvider(android.graphics.Color.GRAY), fontSize = 11.sp)
            )
            Text(
                text = "${monthCount}杯",
                style = TextStyle(
                    color = ColorProvider(android.graphics.Color.BLACK),
                    fontSize = 24.sp,
                    fontWeight = androidx.glance.text.FontWeight.Bold
                )
            )
        }

                // 中间：竖线分隔
        Spacer(
            modifier = GlanceModifier
                .width(1.dp)
                .height(30.dp)
                .background(android.graphics.Color.parseColor("#E0E0E0"))
        )

        // 右侧：约 7 份比例（填满剩余空间）
        Column(
            modifier = GlanceModifier.defaultWeight().padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 绿色小圆点用固定宽的短横线代替（Glance画圆需要额外资源）
                Spacer(modifier = GlanceModifier.width(8.dp).height(8.dp).background(android.graphics.Color.parseColor("#A5D6A7")))
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "${dateStr} 喝过",
                    style = TextStyle(color = ColorProvider(android.graphics.Color.GRAY), fontSize = 12.sp)
                )
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = drinkName,
                style = TextStyle(
                    color = ColorProvider(android.graphics.Color.BLACK),
                    fontSize = 16.sp,
                    fontWeight = androidx.glance.text.FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}
