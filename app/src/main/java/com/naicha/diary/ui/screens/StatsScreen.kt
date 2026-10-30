package com.naicha.diary.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naicha.diary.data.Drink
import com.naicha.diary.ui.components.*
import com.naicha.diary.ui.theme.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun StatsScreen(items: List<Drink>, modifier: Modifier = Modifier) {
    val total = items.size
    val totalSpend = remember(items) { items.sumOf { it.price } }
    val totalCalories = remember(items) { items.sumOf { it.calories } }
    val avgPrice = if (total == 0) 0.0 else totalSpend / total
    val avgRating = if (total == 0) 0f else items.map { it.rating }.sum() / total

    Box(modifier.fillMaxSize()) {
        PearlField(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F5F0)), count = 12, seed = 23)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(modifier = Modifier.statusBarsPadding().padding(top = 12.dp)) {
                    Text(text = "饮品数据 📊", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text(text = if (total == 0) "还没有数据" else "看看你都喝了些什么", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill(label = "总杯数", value = total.toString(), accent = Caramel, modifier = Modifier.weight(1f))
                    StatPill(label = "总花费", value = "¥${formatMoney(totalSpend)}", accent = Taro, modifier = Modifier.weight(1f))
                    StatPill(label = "连续天数", value = "${currentStreak(items)} 天", accent = Matcha, modifier = Modifier.weight(1f))
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill(label = "平均单价", value = "¥${formatMoney(avgPrice)}", accent = Sky, modifier = Modifier.weight(1f))
                    StatPill(label = "平均评分", value = String.format("%.1f", avgRating), accent = Strawberry, modifier = Modifier.weight(1f))
                    StatPill(label = "总热量", value = "${totalCalories}", accent = Color(0xFFE07A5F), modifier = Modifier.weight(1f))
                }
            }

            if (total > 0) {
                item {
                    StatsCard {
                        SectionHeader(title = "品牌分布", subtitle = "喝得最多的那几家")
                        Spacer(Modifier.height(14.dp))
                        BrandDonut(items = items)
                    }
                }

                item {
                    StatsCard {
                        SectionHeader(title = "甜度偏好", subtitle = "你的甜度人格")
                        Spacer(Modifier.height(14.dp))
                        PreferenceBars(
                            entries = listOf("全糖", "七分糖", "半糖", "三分糖", "无糖").map { sugar -> sugar to items.count { it.sugar == sugar } },
                            colors = emptyList()
                        )
                    }
                }

                item {
                    WeekRhythmCard(items)
                }

                item {
                    IntervalCards(items)
                }

                item {
                    SpendingRankCard(items)
                }
            }
        }
    }
}

@Composable
fun StatPill(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(0.05f), spotColor = Color.Black.copy(0.05f))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFCBFF00)))
            Spacer(Modifier.width(6.dp))
            Text(text = label, color = Color(0xFF888888), fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        Text(text = value, color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun StatsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp),
        content = content
    )
}

@Composable
private fun WeekRhythmCard(items: List<Drink>) {
    val weekData = remember(items) {
        val counts = IntArray(7) { 0 }
        items.forEach { drink ->
            val cal = Calendar.getInstance().apply { timeInMillis = drink.timestamp }
            var day = cal.get(Calendar.DAY_OF_WEEK) - 2
            if (day < 0) day = 6
            counts[day]++
        }
        counts.toList()
    }
    val maxCount = weekData.maxOrNull()?.coerceAtLeast(1) ?: 1
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")

    StatsCard {
        SectionHeader(title = "周节律", subtitle = "星期几最忍不住")
        Spacer(Modifier.height(24.dp))
        
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val width = size.width
            val height = size.height
            val stepX = width / 6f
            val paddingY = 40f
            
            val points = weekData.mapIndexed { index, count ->
                val x = index * stepX
                val y = height - paddingY - (count.toFloat() / maxCount * (height - paddingY * 2))
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val cx = (p1.x + p2.x) / 2
                    cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                }
            }
            
            drawPath(path = path, color = Color.Black, style = Stroke(width = 6f, cap = StrokeCap.Round))
            
            points.forEach { point ->
                drawCircle(color = Color(0xFFCBFF00), radius = 12f, center = point)
                drawCircle(color = Color.Black, radius = 12f, center = point, style = Stroke(width = 4f))
            }
        }
        
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label ->
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun IntervalCards(items: List<Drink>) {
    val lastDrinkDays = remember(items) {
        if (items.isEmpty()) return@remember 0
        val last = items.maxOf { it.timestamp }
        val diff = System.currentTimeMillis() - last
        TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }

    val avgDays = remember(items) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        val monthItems = items.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
        }
        val daysInMonth = cal.get(Calendar.DAY_OF_MONTH)
        if (monthItems.isEmpty()) return@remember "0"
        String.format("%.1f", daysInMonth.toFloat() / monthItems.size)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(Color.White).padding(16.dp)) {
            Text("距离上次", color = Color(0xFF888888), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${lastDrinkDays}", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(" 天", color = Color(0xFF888888), fontSize = 14.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        
        Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(Color.White).padding(16.dp)) {
            Text("平均间隔", color = Color(0xFF888888), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${avgDays}", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(" 天/杯", color = Color(0xFF888888), fontSize = 14.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun SpendingRankCard(items: List<Drink>) {
    var period by remember { mutableStateOf("月") }
    val filteredItems = remember(items, period) {
        val now = Calendar.getInstance()
        val startTime = when (period) {
            "周" -> {
                val cal = now.clone() as Calendar
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            "月" -> {
                val cal = now.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            "年" -> {
                val cal = now.clone() as Calendar
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            else -> 0L
        }
        items.filter { it.timestamp >= startTime }
    }

    val totalSpend = filteredItems.sumOf { it.price }
    val brandSpending = remember(filteredItems) {
        filteredItems.groupBy { it.brand }.mapValues { entry -> entry.value.sumOf { it.price } }.toList().sortedByDescending { it.second }.take(4)
    }

    StatsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(title = "消费总额排行", subtitle = null, modifier = Modifier.weight(1f))
            
            Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF4F5F0)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("周", "月", "年").forEach { p ->
                    val selected = period == p
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(if (selected) Color(0xFFCBFF00) else Color.Transparent).clickable { period = p }.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text(text = p, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Color.Black else Color.Gray)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFF4F5F0)), contentAlignment = Alignment.Center) {
                Text("$", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("总计支出", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("¥${formatMoney(totalSpend)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(20.dp))
        
        brandSpending.forEachIndexed { index, (brandName, amount) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${index + 1}", style = MaterialTheme.typography.titleMedium, color = if (index == 0) Color.Black else Color.LightGray, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = brandName, style = MaterialTheme.typography.bodyLarge, color = Color.Black, modifier = Modifier.weight(1f))
                Text(text = "¥${formatMoney(amount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = Color.Black)
            }
        }
    }
}

@Composable
private fun BrandDonut(items: List<Drink>) {
    val data = remember(items) { items.groupingBy { it.brand }.eachCount().entries.sortedByDescending { it.value }.take(6) }
    val total = data.sumOf { it.value }.coerceAtLeast(1)
    val palette = listOf(Caramel, Taro, Matcha, Strawberry, Sky, Color(0xFFF2B04A))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(128.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.24f
                val inset = stroke / 2f
                var startAngle = -90f
                data.forEachIndexed { index, entry ->
                    val sweep = entry.value.toFloat() / total * 360f
                    drawArc(color = palette[index % palette.size], startAngle = startAngle + 1.6f, sweepAngle = (sweep - 3.2f).coerceAtLeast(0.6f), useCenter = false, topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke), style = Stroke(width = stroke, cap = StrokeCap.Round))
                    startAngle += sweep
                }
            }
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedNumber(value = total, style = MaterialTheme.typography.headlineMedium, color = Caramel)
                Text(text = "总杯数", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            data.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(palette[index % palette.size]))
                    Spacer(Modifier.width(7.dp))
                    Text(text = entry.key, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(text = "${entry.value} 杯", style = MaterialTheme.typography.labelMedium, color = PearlSoft)
                }
            }
        }
    }
}

@Composable
private fun PreferenceBars(entries: List<Pair<String, Int>>, colors: List<Color>) {
    val max = entries.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.forEachIndexed { index, (label, count) ->
            val isTop = count == max && count > 0
            val barColor = if (isTop) Color(0xFFCBFF00) else Color(0xFFE5E5E5)
            val textColor = if (isTop) Color.Black else Color(0xFF888888)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, color = textColor, fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(52.dp))
                Box(modifier = Modifier.weight(1f).height(10.dp).clip(CircleShape).background(Color(0xFFF4F5F0))) {
                    Box(modifier = Modifier.fillMaxWidth((count.toFloat() / max).coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(barColor))
                }
                Spacer(Modifier.width(12.dp))
                Text(text = "$count", style = MaterialTheme.typography.bodyMedium, color = textColor, fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}
