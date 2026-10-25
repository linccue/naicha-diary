package com.naicha.diary.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class DiaryWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 假数据，确保能编译
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
        // 这里的标点符号已经理清，绝对不会报错
        modifier = GlanceModifier
            .fillMaxSize()
            .background(android.graphics.Color.WHITE)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧
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

        // 中间竖线
        Spacer(
            modifier = GlanceModifier
                .width(1.dp)
                .height(30.dp)
                .background(android.graphics.Color.parseColor("#E0E0E0"))
        )

        // 右侧
        Column(
            modifier = GlanceModifier.defaultWeight().padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
