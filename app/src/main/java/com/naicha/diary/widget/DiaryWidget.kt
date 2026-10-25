package com.naicha.diary.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.naicha.diary.data.Drink
import com.naicha.diary.data.DrinkRepository
import java.text.SimpleDateFormat
import java.util.*

class DiaryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 读取记录（这里调用你项目自带的存储工具）
        val repository = DrinkRepository(context)
        // 尝试获取数据，如果你的项目中获取数据的方法不是这个，下面会说明怎么改
        val records = repository.getAll().getOrNull() ?: emptyList()

        // 统计本月杯数
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val monthCount = records.count { 
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
        }

        // 获取最近一杯
        val latest = records.maxByOrNull { it.timestamp }
        val dateStr = if (latest != null) {
            SimpleDateFormat("M月d日", Locale.CHINA).format(Date(latest.timestamp))
        } else {
            "--"
        }
        val drinkName = latest?.let {
            if (it.name.isNotBlank()) it.name else it.brand
        } ?: "暂无记录"

        provideContent {
            GlanceTheme {
                WidgetContent(monthCount, dateStr, drinkName)
            }
        }
    }
}

@Composable
private fun WidgetContent(monthCount: Int, dateStr: String, drinkName: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(android.graphics.Color.WHITE))
            .padding(14.dp)
            .appWidgetBackground(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：3份比例（本月杯数）
        Column(
            modifier = GlanceModifier.fillMaxWidth(0.3f).padding(end = 8.dp),
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

        // 中间：微弱的竖线分隔
        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .height(30.dp)
                .background(ColorProvider(android.graphics.Color.parseColor("#E0E0E0")))
        ) {}

        // 右侧：7份比例（最近一杯）
        Column(
            modifier = GlanceModifier.fillMaxWidth(0.7f).padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp).background(ColorProvider(android.graphics.Color.parseColor("#A5D6A7"))))
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
