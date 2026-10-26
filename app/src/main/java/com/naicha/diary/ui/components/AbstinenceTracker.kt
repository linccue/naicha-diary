package com.naicha.diary.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AbstinenceTracker(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("abstinence_prefs", android.content.Context.MODE_PRIVATE) }

    // 读取状态
    var isTracking by remember { mutableStateOf(prefs.getBoolean("is_tracking", false)) }
    var targetDays by remember { mutableIntStateOf(prefs.getInt("target_days", 7)) }
    var startDate by remember { mutableLongStateOf(prefs.getLong("start_date", 0L)) }
    var drankDaysStr by remember { mutableStateOf(prefs.getString("drank_days", "") ?: "") }
    
    // 当前的选项目标（准备阶段）
    var selectedGoal by remember { mutableIntStateOf(7) }
    
    // 是否今天喝了
    val drankDays = remember(drankDaysStr) {
        if (drankDaysStr.isEmpty()) emptySet() else drankDaysStr.split(",").map { it.toInt() }.toSet()
    }

    // 计算当前是第几天 (0开始)
    val todayDiff = remember(startDate) {
        if (startDate == 0L) 0
        else {
            val cal1 = Calendar.getInstance().apply { timeInMillis = startDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            val cal2 = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            ((cal2.timeInMillis - cal1.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        }
    }

    val isTodayDrank = drankDays.contains(todayDiff)

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // 标题
        Text(
            text = "不喝打卡",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (!isTracking) {
            // ============ 准备阶段 (图一) ============
            
            // 深色卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF3D3D3D))
                    .padding(20.dp)
            ) {
                Column {
                    Text("设一个连续天数，从今天开始", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(Color.Black)
                            .clickable {
                                isTracking = true
                                targetDays = selectedGoal
                                startDate = System.currentTimeMillis()
                                drankDaysStr = ""
                                prefs.edit()
                                    .putBoolean("is_tracking", true)
                                    .putInt("target_days", selectedGoal)
                                    .putLong("start_date", System.currentTimeMillis())
                                    .putString("drank_days", "")
                                    .apply()
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("开始 $selectedGoal 天", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 白色卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Column {
                    Text("选择目标", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    val goals = listOf(3, 7, 15, 21, 30)
                    // 用 FlowRow 效果展示按钮 (按两排)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        goals.take(3).forEach { goal ->
                            GoalChip(
                                text = if (goal < 7) "${goal} 天" else if (goal == 7) "1 周" else "${goal} 天",
                                selected = selectedGoal == goal,
                                onClick = { selectedGoal = goal }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        goals.drop(3).forEach { goal ->
                            GoalChip(
                                text = if (goal == 30) "1 个月" else "${goal} 天",
                                selected = selectedGoal == goal,
                                onClick = { selectedGoal = goal }
                            )
                        }
                    }
                }
            }

        } else {
            // ============ 进行中阶段 (图二/三/四) ============
            
            // 深色卡片 (进度)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF3D3D3D))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isTodayDrank) "今天喝了" else "已连续不喝 ${todayDiff - drankDays.count { it < todayDiff }} 天",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    isTracking = false
                                    prefs.edit().putBoolean("is_tracking", false).apply()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("结束", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // 进度条
                    val progress = (todayDiff.toFloat() / targetDays).coerceIn(0f, 1f)
                    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF555555))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0))
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "进度 ${todayDiff + 1}/$targetDays · 还差 ${targetDays - todayDiff - 1} 天",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 白色卡片 (本轮进度圆圈)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Column {
                    Text("本轮进度", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(16.dp))
                    
                    // 渲染圆圈矩阵 (每排 7 个)
                    val circles = (0 until targetDays).chunked(7)
                    circles.forEach { rowList ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                            rowList.forEach { dayIndex ->
                                val circleColor = when {
                                    drankDays.contains(dayIndex) -> Color(0xFFF2A54A) // 橙
                                    dayIndex <= todayDiff -> Color(0xFF52D17C) // 绿
                                    else -> Color(0xFFF2F2F2) // 白/灰
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(circleColor)
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    // 图例
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendItem("不喝", Color(0xFF52D17C))
                        LegendItem("喝了", Color(0xFFF2A54A))
                        LegendItem("未到", Color(0xFFF2F2F2))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 统计数据
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val validStreak = if (isTodayDrank) 0 else todayDiff - drankDays.count { it < todayDiff }
                StatBox("已连续", "${validStreak} 天", Modifier.weight(1f))
                StatBox("累计不喝", "${(todayDiff + 1) - drankDays.size} 天", Modifier.weight(1f))
                
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(startDate))
                StatBox("开始于", dateStr, Modifier.weight(1.2f))
            }

            Spacer(Modifier.height(16.dp))

            // 底部打卡按钮
            val btnBgColor by animateColorAsState(if (isTodayDrank) Color.Black else Color.White, label = "btnBg")
            val btnTextColor by animateColorAsState(if (isTodayDrank) Color.White else Color.Black, label = "btnText")
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(btnBgColor)
                    .border(1.dp, if (isTodayDrank) Color.Transparent else Color.LightGray, CircleShape)
                    .clickable {
                        val newDrankDays = if (isTodayDrank) {
                            drankDays - todayDiff
                        } else {
                            drankDays + todayDiff
                        }
                        drankDaysStr = newDrankDays.joinToString(",")
                        prefs.edit().putString("drank_days", drankDaysStr).apply()
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isTodayDrank) "其实没喝" else "今天喝了",
                    color = btnTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GoalChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color.Black else Color(0xFFF5F5F5))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.DarkGray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text = text, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun StatBox(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(text = title, color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(text = value, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
