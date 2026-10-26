package com.naicha.diary.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
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
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AbstinenceTracker(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {} // 新增：打开设置的点击事件
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("abstinence_prefs", android.content.Context.MODE_PRIVATE) }

    var isTracking by remember { mutableStateOf(prefs.getBoolean("is_tracking", false)) }
    var targetDays by remember { mutableIntStateOf(prefs.getInt("target_days", 7)) }
    var startDate by remember { mutableLongStateOf(prefs.getLong("start_date", 0L)) }
    var drankDaysStr by remember { mutableStateOf(prefs.getString("drank_days", "") ?: "") }
    
    var selectedGoal by remember { mutableIntStateOf(7) }
    var showEndDialog by remember { mutableStateOf(false) } // 结束弹窗控制

    val drankDays = remember(drankDaysStr) {
        if (drankDaysStr.isEmpty()) emptySet() else drankDaysStr.split(",").map { it.toInt() }.toSet()
    }

    val todayDiff = remember(startDate) {
        if (startDate == 0L) 0
        else {
            val cal1 = Calendar.getInstance().apply { timeInMillis = startDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            val cal2 = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            ((cal2.timeInMillis - cal1.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        }
    }

    val isTodayDrank = drankDays.contains(todayDiff)

    Column(modifier = modifier.fillMaxWidth()) {
        // 标题和齿轮按钮
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "不喝打卡",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            // 只有在第一层页面才显示齿轮按钮
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (!isTracking) {
            // ============ 准备阶段 ============
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF3D3D3D))
                    .padding(20.dp)
            ) {
                Column {
                    Text("设一个连续天数，从今天开始", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center)
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
                    // 允许横向滑动，保证所有选项在一行
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        goals.forEach { goal ->
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
            // ============ 进行中阶段 ============
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
                                .clickable { showEndDialog = true } // 点击显示弹窗
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("结束", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
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
                    
                    val circles = (0 until targetDays).chunked(7)
                    circles.forEach { rowList ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                            rowList.forEach { dayIndex ->
                                val circleColor = when {
                                    drankDays.contains(dayIndex) -> Color(0xFFF2A54A) // 橙
                                    dayIndex <= todayDiff -> Color(0xFF52D17C) // 绿
                                    else -> Color(0xFFF2F2F2) // 灰
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
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendItem("不喝", Color(0xFF52D17C))
                        LegendItem("喝了", Color(0xFFF2A54A))
                        LegendItem("未到", Color(0xFFF2F2F2))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val validStreak = if (isTodayDrank) 0 else todayDiff - drankDays.count { it < todayDiff }
                StatBox("已连续", "${validStreak} 天", Modifier.weight(1f))
                StatBox("累计不喝", "${(todayDiff + 1) - drankDays.size} 天", Modifier.weight(1f))
                
                // 去掉了年份，格式变为 MM-dd
                val dateStr = SimpleDateFormat("MM-dd", Locale.CHINA).format(Date(startDate))
                StatBox("开始于", dateStr, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            val btnBgColor by animateColorAsState(if (isTodayDrank) Color.Black else Color.White, label = "btnBg")
            val btnTextColor by animateColorAsState(if (isTodayDrank) Color.White else Color.Black, label = "btnText")
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(btnBgColor)
                    .border(1.dp, if (isTodayDrank) Color.Transparent else Color.LightGray, CircleShape)
                    .clickable {
                        val newDrankDays = if (isTodayDrank) drankDays - todayDiff else drankDays + todayDiff
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

    // 结束确认弹窗
    if (showEndDialog) {
        Dialog(onDismissRequest = { showEndDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("结束本轮？", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(24.dp).clickable { showEndDialog = false }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("结束后当前进度会清空。", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(24.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                                .background(Color.White)
                                .clickable { showEndDialog = false }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("取消", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .clickable {
                                    isTracking = false
                                    prefs.edit().putBoolean("is_tracking", false).apply()
                                    showEndDialog = false
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("结束", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
            fontSize = 14.sp,
            maxLines = 1
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
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally // 👈 强制内容居中
    ) {
        Text(
            text = title, 
            color = Color.Gray, 
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value, 
            color = Color.Black, 
            fontSize = 16.sp, 
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
