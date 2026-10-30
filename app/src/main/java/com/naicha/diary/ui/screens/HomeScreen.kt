package com.naicha.diary.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naicha.diary.R
import com.naicha.diary.data.Catalog
import com.naicha.diary.data.Drink
import com.naicha.diary.data.DrinkCategory
import com.naicha.diary.ui.components.*
import com.naicha.diary.ui.theme.*
import com.naicha.diary.util.PhotoStore
import com.naicha.diary.util.TimeUtil
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    items: List<Drink>,
    onRecord: () -> Unit,
    onAiRecord: () -> Unit,
    onOpenWall: () -> Unit,
    onOpenDetail: (Drink) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showCategory by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    
    var selectedCategory by remember { mutableStateOf("全部") }
    var sortOrder by remember { mutableStateOf("默认") }
    var searchQuery by remember { mutableStateOf("") }
    var activeQuickFilter by remember { mutableStateOf<String?>(null) }

    // 控制成就弹窗
    var showAchievements by remember { mutableStateOf(false) }
    val achievementSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 读取不喝打卡状态
    val prefs = remember { context.getSharedPreferences("abstinence_prefs", android.content.Context.MODE_PRIVATE) }
    val isTracking = prefs.getBoolean("is_tracking", false)
    val targetDays = prefs.getInt("target_days", 7)
    val startDate = prefs.getLong("start_date", 0L)
    val drankDaysStr = prefs.getString("drank_days", "") ?: ""
    val drankDays = remember(drankDaysStr) { if (drankDaysStr.isEmpty()) emptySet() else drankDaysStr.split(",").map { it.toInt() }.toSet() }
    
    val todayDiff = remember(startDate) {
        if (startDate == 0L) 0 else {
            val cal1 = Calendar.getInstance().apply { timeInMillis = startDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            val cal2 = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            ((cal2.timeInMillis - cal1.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        }
    }
    val validStreak = if (drankDays.contains(todayDiff)) 0 else todayDiff - drankDays.count { it < todayDiff }

    // 数据统计
    val totalCount = items.size
    val monthItems = remember(items) {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        items.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
        }
    }
    val monthCount = monthItems.size
    val monthSpend = monthItems.sumOf { it.price }
    
    // 计算本周记录
    val weekItemsCount = remember(items) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        items.count { it.timestamp >= cal.timeInMillis }
    }

    val processedItems = remember(items, selectedCategory, sortOrder, searchQuery, activeQuickFilter) {
        var result = items

        if (selectedCategory != "全部") {
            result = result.filter { it.category == selectedCategory }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            result = result.filter {
                it.brand.contains(q, ignoreCase = true) ||
                it.name.contains(q, ignoreCase = true) ||
                it.toppings.any { t -> t.contains(q, ignoreCase = true) }
            }
        }

        when (activeQuickFilter) {
            "无限复购" -> {
                val counts = items.groupingBy { it.brand + it.name }.eachCount()
                result = result.filter { counts[it.brand + it.name] ?: 0 > 3 }
            }
            "心头好" -> result = result.filter { it.rating == 5f }
            "本月" -> result = result.filter { mItem -> monthItems.any { it.id == mItem.id } }
            "本周" -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                result = result.filter { it.timestamp >= cal.timeInMillis }
            }
        }

        result = when (sortOrder) {
            "评分由高到低" -> result.sortedByDescending { it.rating }
            "时间顺序" -> result.sortedByDescending { it.timestamp }
            else -> result.reversed()
        }

        result.take(30)
    }

    Box(modifier.fillMaxSize()) {
        PearlField(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF4F5F0)),
            count = 14,
            seed = 3,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 132.dp),
        ) {
            // 1. 顶部打卡状态栏
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val titleText = if (isTracking) {
                            "不喝打卡第 ${todayDiff + 1}/$targetDays 天，\n继续保持>"
                        } else {
                            "今天喝点什么呢？\n去记一杯吧>"
                        }
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            lineHeight = 32.sp,
                            color = Color.Black
                        )
                    }
                    // 👇 右侧头像点击，触发独立弹窗
                    Box(modifier = Modifier.clip(CircleShape).clickable { showAchievements = true }) {
                        Image(
                            painter = painterResource(id = R.drawable.girl_avatar),
                            contentDescription = "头像",
                            modifier = Modifier.size(64.dp).clip(CircleShape).border(1.dp, Color(0xFFE0E0E0), CircleShape)
                        )
                    }
                }
            }

            // 2. 三块数据统计卡片
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(172.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 左侧大卡片：本月已喝
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(text = "本月已喝", fontSize = 13.sp, color = Color(0xFF888888), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(text = "$monthCount", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.Black, lineHeight = 44.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(text = "杯", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(bottom = 6.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            // 底部灰色胶囊标签
                            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF0F0F0)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text(text = "总记录数量：$totalCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        // 背景透明杯子水印
                        Image(
                            painter = painterResource(id = R.drawable.drink_milk_tea_cold),
                            contentDescription = null,
                            alpha = 0.05f,
                            modifier = Modifier.size(100.dp).align(Alignment.CenterEnd).offset(x = 20.dp, y = 10.dp)
                        )
                    }

                    // 右侧上下卡片
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 右侧上：本月花费
                        Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "本月花费", fontSize = 13.sp, color = Color(0xFF888888), fontWeight = FontWeight.Medium)
                                Text(text = "¥${formatMoney(monthSpend)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black, lineHeight = 26.sp)
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // 右侧下：平均花费
                        Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "平均花费", fontSize = 13.sp, color = Color(0xFF888888), fontWeight = FontWeight.Medium)
                                Text(text = "¥${formatMoney(if (monthCount > 0) monthSpend / monthCount else 0.0)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black, lineHeight = 26.sp)
                            }
                        }
                    }
                }
            }

            // 3. 最近喝过标题与过滤
            item {
                Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "最近喝过", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.weight(1f))
                        
                        IconCircleButton(icon = { Icon(Icons.Default.Menu, "分类", tint = if (showCategory) Color.White else Color.Black) }, isActive = showCategory, onClick = { showCategory = !showCategory; if (showCategory) showSearch = false })
                        Spacer(Modifier.width(8.dp))
                        
                        Box {
                            IconCircleButton(icon = { Text("⇅", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (showSortMenu) Color.White else Color.Black) }, isActive = showSortMenu, onClick = { showSortMenu = true })
                            DropdownMenu(
                                expanded = showSortMenu, onDismissRequest = { showSortMenu = false },
                                containerColor = Color.Transparent, tonalElevation = 0.dp, shadowElevation = 0.dp, modifier = Modifier.background(Color.Transparent)
                            ) {
                                Column(modifier = Modifier.padding(8.dp).shadow(12.dp, RoundedCornerShape(24.dp)).background(Color(0xFFF4F5F7), RoundedCornerShape(24.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("默认", "评分由高到低", "时间顺序").forEach { option ->
                                        val isSelected = sortOrder == option
                                        Box(
                                            modifier = Modifier.fillMaxWidth().shadow(if (isSelected) 6.dp else 1.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Black.copy(alpha = 0.1f)).clip(RoundedCornerShape(20.dp)).background(if (isSelected) Color.Black else Color.White).clickable { sortOrder = option; showSortMenu = false }.padding(horizontal = 20.dp, vertical = 16.dp)
                                        ) {
                                            Text(text = option, color = if (isSelected) Color.White else Color.Black, style = MaterialTheme.typography.titleMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        
                        IconCircleButton(icon = { Icon(Icons.Default.Search, "搜索", tint = if (showSearch) Color.White else Color.Black) }, isActive = showSearch, onClick = { showSearch = !showSearch; if (!showSearch) { searchQuery = ""; activeQuickFilter = null }; if (showSearch) showCategory = false })
                    }
                    
                    AnimatedVisibility(visible = showCategory) {
                        FlowRow(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("全部", "奶茶", "果茶", "咖啡", "柠檬茶", "纯茶", "瓶装", "酒类", "酸奶").forEach { cat ->
                                FilterChipCustom(text = cat, selected = selectedCategory == cat, onClick = { selectedCategory = cat })
                            }
                        }
                    }

                    AnimatedVisibility(visible = showSearch) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = searchQuery, onValueChange = { searchQuery = it },
                                placeholder = { Text("搜索品牌/品项/加料", color = Color.Gray) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Black) },
                                trailingIcon = { if (searchQuery.isNotEmpty()) { Icon(Icons.Default.Close, "清空", modifier = Modifier.clickable { searchQuery = "" }) } },
                                singleLine = true, shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("无限复购", "心头好", "本月", "本周").forEach { filter ->
                                    FilterChipCustom(text = filter, selected = activeQuickFilter == filter, onClick = { activeQuickFilter = if (activeQuickFilter == filter) null else filter })
                                }
                            }
                        }
                    }
                }
            }

            // 4. 列表
            if (processedItems.isEmpty()) {
                item { EmptyHint(onRecord = onRecord) }
            } else {
                items(processedItems, key = { it.id }) { tea ->
                    TeaRow(tea = tea, onClick = { onOpenDetail(tea) }, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
                }
            }
        }
        
        // 👇 独立二级窗口：点击头像弹出的成就勋章
        if (showAchievements) {
            ModalBottomSheet(
                onDismissRequest = { showAchievements = false },
                sheetState = achievementSheetState,
                containerColor = Color(0xFFF4F5F0),
                dragHandle = {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(44.dp).height(5.dp).clip(CircleShape).background(Color.LightGray))
                    }
                }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text(
                        text = "我的成就 🏆",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(16.dp))
                    SoftCard {
                        SectionHeader(title = "成就勋章", subtitle = "解锁你的饮品成就")
                        Spacer(Modifier.height(14.dp))
                        Achievements(items = items, streak = currentStreak(items))
                    }
                    Spacer(Modifier.height(40.dp)) // 底部留白，防遮挡
                }
            }
        }
    }
}

// ================= 辅助组件与函数 =================

@Composable
private fun IconCircleButton(icon: @Composable () -> Unit, isActive: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isActive) Color.Black else Color.Transparent).border(1.dp, if (isActive) Color.Black else Color.LightGray, CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) { icon() }
}

@Composable
private fun FilterChipCustom(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(if (selected) Color.Black else Color.Transparent).border(1.dp, if (selected) Color.Black else Color.LightGray, RoundedCornerShape(50)).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else Color.Gray)
    }
}

@Composable
private fun TeaRow(tea: Drink, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val brand = Catalog.brandOf(tea.brand)
    val displayName = tea.name.ifBlank { if (tea.category == "咖啡") "神秘咖啡" else "神秘${tea.category}" }

    BouncyBox(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    if (tea.photoPath != null) {
                        val file = PhotoStore.fileOf(tea.photoPath)
                        val bitmap = remember(file) { 
                            val raw = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                            if (raw != null) trimTransparentEdges(raw) else null 
                        }
                        if (bitmap != null) {
                            val imageBitmap = bitmap.asImageBitmap()
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                val imgWidth = imageBitmap.width.toFloat()
                                val imgHeight = imageBitmap.height.toFloat()
                                val canvasRatio = size.width / size.height
                                val imgRatio = imgWidth / imgHeight
                                val drawWidth: Float; val drawHeight: Float
                                if (canvasRatio > imgRatio) { drawHeight = size.height; drawWidth = drawHeight * imgRatio } else { drawWidth = size.width; drawHeight = drawWidth / imgRatio }
                                val dx = (size.width - drawWidth) / 2f; val dy = (size.height - drawHeight) / 2f
                                val dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt()); val dstOffset = IntOffset(dx.toInt(), dy.toInt())
                                val strokePx = 2.dp.toPx().toInt()
                                val offsets = listOf(IntOffset(-strokePx, -strokePx), IntOffset(0, -strokePx), IntOffset(strokePx, -strokePx), IntOffset(-strokePx, 0), IntOffset(strokePx, 0), IntOffset(-strokePx, strokePx), IntOffset(0, strokePx), IntOffset(strokePx, strokePx))
                                offsets.forEach { offset -> drawImage(image = imageBitmap, srcOffset = IntOffset.Zero, srcSize = IntSize(imageBitmap.width, imageBitmap.height), dstOffset = IntOffset(dstOffset.x + offset.x, dstOffset.y + offset.y), dstSize = dstSize, colorFilter = ColorFilter.tint(Color.White), filterQuality = FilterQuality.High) }
                                drawImage(image = imageBitmap, srcOffset = IntOffset.Zero, srcSize = IntSize(imageBitmap.width, imageBitmap.height), dstOffset = dstOffset, dstSize = dstSize, colorFilter = null, filterQuality = FilterQuality.High)
                            }
                        } else {
                            LocalPhoto(file = file, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        Image(painter = painterResource(id = getDrinkIconRes(tea)), contentDescription = null, modifier = Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit)
                    }
                    Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).background(Color.White, CircleShape).padding(2.dp)) {
                        BrandBadge(badge = brand.badge, color = Color(brand.color), size = 20.dp, logo = brand.logo, squircle = Catalog.isSquircle(brand.name))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = displayName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF4F5F0)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(text = TimeUtil.dayLabel(tea.timestamp), style = MaterialTheme.typography.labelSmall, color = PearlSoft)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(text = tea.brand.ifBlank { "未知品牌" }, style = MaterialTheme.typography.bodyMedium, color = PearlSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    RatingStars(rating = tea.rating, starSize = 14.dp)
                    val tags = mutableListOf<String>()
                    if (tea.sugar.isNotBlank()) tags.add(tea.sugar)
                    if (tea.ice.isNotBlank()) tags.add(tea.ice)
                    if (tea.toppings.isNotEmpty()) tags.addAll(tea.toppings)
                    if (tags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            tags.forEach { tag ->
                                Box(modifier = Modifier.clip(RoundedCornerShape(50)).border(0.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(50)).background(Color.White).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text(text = tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFF0F0F0)))
        }
    }
}

@Composable
private fun EmptyHint(onRecord: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        DrinkCup(modifier = Modifier.width(120.dp).height(150.dp), liquidColor = Color(0xFFDCC7B4), strawColor = Color(0xFFC9B5A3), toppingCount = 3, style = CupStyle.Tea)
        Spacer(Modifier.height(14.dp))
        Text(text = "还没有记录哦", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(text = "喝下的每一杯，都值得被记住", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// 成就勋章组件
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        badges.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (emoji, label, unlocked) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (unlocked) Caramel.copy(alpha = 0.15f) else Color(0xFFF4F5F0))
                            .border(if (unlocked) 0.dp else 1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = if (unlocked) emoji else "🔒", fontSize = 24.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (unlocked) Color.Black else Color(0xFFBBBBBB),
                            fontWeight = if (unlocked) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// 连续天数计算
private fun currentStreak(items: List<Drink>): Int {
    if (items.isEmpty()) return 0
    val days = items.map { TimeUtil.startOfDay(it.timestamp) }.toHashSet()
    var cursor = TimeUtil.startOfDay(System.currentTimeMillis())
    if (!days.contains(cursor)) cursor -= 86_400_000L
    var streak = 0
    while (days.contains(cursor)) { streak++; cursor -= 86_400_000L }
    return streak
}

fun formatMoney(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.CHINA, "%.1f", value)

fun trimTransparentEdges(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
    var top = bitmap.height; var bottom = 0; var left = bitmap.width; var right = 0
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            val alpha = (pixels[y * bitmap.width + x] shr 24) and 0xff
            if (alpha > 10) {
                if (x < left) left = x; if (x > right) right = x; if (y < top) top = y; if (y > bottom) bottom = y
            }
        }
    }
    return if (top < bottom && left < right) android.graphics.Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1) else bitmap
}

fun getDrinkIconRes(tea: Drink): Int {
    val isHot = tea.ice.contains("热") || tea.ice.contains("常温")
    if (tea.brand in listOf("瑞幸咖啡", "星巴克", "库迪咖啡", "Manner", "M Stand", "Tims", "Peet's", "挪瓦咖啡", "幸运咖")) {
        return if (isHot) R.drawable.drink_coffee_hot else R.drawable.drink_coffee_cold
    }
    val category = DrinkCategory.of(tea.category)
    return when (category) {
        DrinkCategory.MilkTea -> if (isHot) R.drawable.drink_milk_tea_hot else R.drawable.drink_milk_tea_cold
        DrinkCategory.Coffee -> if (isHot) R.drawable.drink_coffee_hot else R.drawable.drink_coffee_cold
        DrinkCategory.FruitTea -> R.drawable.drink_fruit_tea
        DrinkCategory.LemonTea -> R.drawable.drink_lemon_tea
        DrinkCategory.PureTea -> R.drawable.drink_pure_tea
        DrinkCategory.Bottled -> R.drawable.drink_bottle
        DrinkCategory.Alcohol -> R.drawable.drink_alcohol
        else -> R.drawable.drink_general
    }
}
