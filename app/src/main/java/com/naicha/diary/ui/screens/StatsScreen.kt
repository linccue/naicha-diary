package com.naicha.diary.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
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
import com.naicha.diary.data.Catalog
import com.naicha.diary.data.Drink
import com.naicha.diary.ui.components.*
import com.naicha.diary.ui.theme.*
import com.naicha.diary.util.TimeUtil
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun StatsScreen(items: List<Drink>, modifier: Modifier = Modifier) {
    val total = items.size
    val totalSpend = remember(items) { items.sumOf { it.price } }
    val totalCalories = remember(items) { items.sumOf { it.calories } }
    val avgPrice = if (total == 0) 0.0 else totalSpend / total
    val streak = remember(items) { currentStreak(items) }
    val avgRating = if (total == 0) 0f else items.sumOf { it.rating }.toFloat() / total

    Box(modifier.fillMaxSize()) {
        PearlField(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), count = 12, seed = 23)

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
                   @Composable
fun StatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            // 1. 大圆角 + 带微主题色的柔和阴影（让卡片浮起来，且不显脏）
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = accent.copy(alpha = 0.25f),
                spotColor = accent.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(18.dp))
            // 2. 极其轻微的白色到主题浅色的渐变底，比纯白更有质感
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        accent.copy(alpha = 0.06f)
                    )
                )
            )
            // 3. 加一圈极细的主题色边框，强化轮廓
            .border(0.8.dp, accent.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 14.dp)
    ) {
        // 顶部：小圆点 + 标签
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                color = Color(0xFF888888), // 统一的灰色标签
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        
        Spacer(Modifier.height(10.dp))
        
        // 底部：超大、超粗的深色数字，极其显眼
        Text(
            text = value,
            color = Color(0xFF1E1E1E),
            fontSize = 22.sp, 
            fontWeight = FontWeight.Black, 
            maxLines = 1
        )
    }
}
            }

            if (total > 0) {
                // 1. 品牌分布
                item {
                    SoftCard {
                        SectionHeader(title = "品牌分布", subtitle = "喝得最多的那几家")
                        Spacer(Modifier.height(14.dp))
                        BrandDonut(items = items)
                    }
                }

                // 2. 甜度偏好
                item {
                    SoftCard {
                        SectionHeader(title = "甜度偏好", subtitle = "你的甜度人格")
                        Spacer(Modifier.height(14.dp))
                        PreferenceBars(
                            entries = Catalog.sugars.map { sugar -> sugar to items.count { it.sugar == sugar } },
                            colors = listOf(Strawberry, Color(0xFFF2B04A), Caramel, Matcha, Sky),
                        )
                    }
                }

                // 3. 本月日历
                item {
                    SoftCard {
                        SectionHeader(title = "本月日历", subtitle = "每天喝了几杯")
                        Spacer(Modifier.height(14.dp))
                        MonthHeatmap(items = items)
                    }
                }

                // 4. 周节律
                item {
                    WeekRhythmCard(items)
                }

                // 5. 距离上次与平均天数
                item {
                    IntervalCards(items)
                }

                // 6. 消费总额排行
                item {
                    SpendingRankCard(items)
                }

                // 7. 成就勋章（确保在所有卡片的最下方）
                item {
                    SoftCard {
                        SectionHeader(title = "成就勋章", subtitle = "解锁你的饮品成就")
                        Spacer(Modifier.height(14.dp))
                        Achievements(items = items, streak = streak)
                    }
                }
            }
        }
    }
}

// ================= 各模块组件逻辑 =================

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

    SoftCard {
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
                drawCircle(color = Color(0xFFCCFF00), radius = 12f, center = point)
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

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(16.dp)) {
            Text("距离上次喝奶茶已经\n${lastDrinkDays} 天", fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
        }
        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(16.dp)) {
            Text("本月平均每 ${avgDays} 天喝\n一杯", fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
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
        filteredItems.groupBy { it.brand }
            .mapValues { entry -> entry.value.sumOf { it.price } }
            .toList()
            .sortedByDescending { it.second }
            .take(4)
    }

    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(title = "消费总额排行", subtitle = null, modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF4F5F7)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("周", "月", "年").forEach { p ->
                    val selected = period == p
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Color(0xFFCCFF00) else Color.Transparent)
                            .clickable { period = p }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = p,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.Black else Color.Gray
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE8E9EB)), contentAlignment = Alignment.Center) {
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (index == 0) Color.Black else Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = brandName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "¥${formatMoney(amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
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
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        entries.forEachIndexed { index, (label, count) ->
            val color = colors[index % colors.size]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(52.dp))
                Box(modifier = Modifier.weight(1f).height(14.dp).clip(CircleShape).background(color.copy(alpha = 0.13f))) {
                    Box(modifier = Modifier.fillMaxWidth((count.toFloat() / max).coerceIn(0.04f, 1f)).height(14.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f)))))
                }
                Spacer(Modifier.width(9.dp))
                Text(text = "$count", style = MaterialTheme.typography.labelMedium, color = PearlSoft, modifier = Modifier.width(24.dp))
            }
        }
    }
}

@Composable
private fun MonthHeatmap(items: List<Drink>) {
    val calendar = remember { Calendar.getInstance() }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val today = calendar.get(Calendar.DAY_OF_MONTH)
    val dayRecords = remember(items, year, month) {
        items.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
        }.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.DAY_OF_MONTH)
        }
    }
    val first = Calendar.getInstance().apply { set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1) }
    val leading = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        val cells = leading + daysInMonth
        val rows = (cells + 6) / 7
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val index = row * 7 + col
                    val day = index - leading + 1
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(6.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..daysInMonth) {
                            val drinksOfDay = dayRecords[day]
                            if (!drinksOfDay.isNullOrEmpty()) {
                                val brand = Catalog.brandOf(drinksOfDay.first().brand)
                                BrandBadge(badge = brand.badge, color = Color(brand.color), size = 32.dp, logo = brand.logo, squircle = Catalog.isSquircle(brand.name))
                            } else {
                                Text(text = day.toString(), style = MaterialTheme.typography.titleMedium, color = if (day == today) Caramel else MaterialTheme.colorScheme.onSurface, fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal)
                            }
                        } else {
                            Spacer(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Achievements(items: List<Drink>, streak: Int) {
    val brandCount = items.map { it.brand }.distinct().size
    val sugarCount = items.map { it.sugar }.distinct().size
    val photoCount = items.count { it.photoPath != null }
    val total = items.size
    val badges = listOf(
        Triple("🌱", "第一杯", total >= 1), Triple("🧋", "十杯达成", total >= 10),
        Triple("🏅", "五十杯", total >= 50), Triple("👑", "百杯王者", total >= 100),
        Triple("🔥", "连续三天", streak >= 3), Triple("⚡", "连续七天", streak >= 7),
        Triple("🧭", "尝鲜达人", brandCount >= 5), Triple("🍬", "甜度全通", sugarCount >= 5),
        Triple("📷", "拍照记录", photoCount >= 5),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        badges.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (emoji, label, unlocked) ->
                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (unlocked) Caramel.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surfaceContainerLow).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (unlocked) emoji else "🔒", fontSize = 22.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(text = label, style = MaterialTheme.typography.labelMedium, color = if (unlocked) Caramel else PearlSoft, fontWeight = if (unlocked) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun currentStreak(items: List<Drink>): Int {
    if (items.isEmpty()) return 0
    val days = items.map { TimeUtil.startOfDay(it.timestamp) }.toHashSet()
    var cursor = TimeUtil.startOfDay(System.currentTimeMillis())
    if (!days.contains(cursor)) cursor -= 86_400_000L
    var streak = 0
    while (days.contains(cursor)) { streak++; cursor -= 86_400_000L }
    return streak
}
