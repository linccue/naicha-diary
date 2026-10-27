package com.naicha.diary.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naicha.diary.R
import com.naicha.diary.data.Catalog
import com.naicha.diary.data.Drink
import com.naicha.diary.data.DrinkCategory
import com.naicha.diary.ui.components.AnimatedNumber
import com.naicha.diary.ui.components.BouncyBox
import com.naicha.diary.ui.components.BrandBadge
import com.naicha.diary.ui.components.DrinkCup
import com.naicha.diary.ui.components.cupStyleFor
import com.naicha.diary.ui.components.CupStyle // 👈 加上了这行
import com.naicha.diary.ui.components.PearlField
import com.naicha.diary.ui.components.RatingStars
import com.naicha.diary.ui.components.LocalPhoto
import com.naicha.diary.ui.theme.Caramel
import com.naicha.diary.ui.theme.PearlSoft
import com.naicha.diary.ui.theme.Strawberry
import com.naicha.diary.ui.theme.Taro
import com.naicha.diary.util.PhotoStore
import com.naicha.diary.util.TimeUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    items: List<Drink>,
    onRecord: () -> Unit,
    onAiRecord: () -> Unit,
    onOpenWall: () -> Unit,
    onOpenDetail: (Drink) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCategory by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    
    var selectedCategory by remember { mutableStateOf("全部") }
    var sortOrder by remember { mutableStateOf("默认") }
    var searchQuery by remember { mutableStateOf("") }
    var activeQuickFilter by remember { mutableStateOf<String?>(null) }

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
    val dateLabel = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date()) }

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
            "心头好" -> result = result.filter { it.rating == 5 }
            "本月" -> result = result.filter { mItem -> monthItems.any { it.id == mItem.id } }
            "本周" -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val weekStart = cal.timeInMillis
                result = result.filter { it.timestamp >= weekStart }
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
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F5F0)),
            count = 14,
            seed = 3,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 132.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "${TimeUtil.greeting()} 🧋",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                HeroCard(
                    monthCount = monthCount,
                    monthSpend = monthSpend,
                    todayStyle = monthItems.maxByOrNull { it.timestamp }?.categoryType ?: DrinkCategory.MilkTea,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                )
            }

            item {
                QuickActions(
                    onRecord = onRecord,
                    onAiRecord = onAiRecord,
                    onOpenWall = onOpenWall,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "最近喝过",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.weight(1f))
                        
                        IconCircleButton(
                            icon = { Icon(Icons.Default.Menu, "分类", tint = if (showCategory) Color.White else Color.Black) },
                            isActive = showCategory,
                            onClick = { 
                                showCategory = !showCategory
                                if (showCategory) showSearch = false
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        
                        Box {
                            IconCircleButton(
                                icon = { 
                                    Text(
                                        text = "⇅", 
                                        fontSize = 18.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = if (showSortMenu) Color.White else Color.Black
                                    ) 
                                },
                                isActive = showSortMenu,
                                onClick = { showSortMenu = true }
                            )
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                modifier = Modifier.background(Color.Transparent)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .shadow(12.dp, RoundedCornerShape(24.dp))
                                        .background(Color(0xFFF4F5F7), RoundedCornerShape(24.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("默认", "评分由高到低", "时间顺序").forEach { option ->
                                        val isSelected = sortOrder == option
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .shadow(
                                                    elevation = if (isSelected) 6.dp else 1.dp,
                                                    shape = RoundedCornerShape(20.dp),
                                                    ambientColor = Color.Black.copy(alpha = 0.1f),
                                                    spotColor = Color.Black.copy(alpha = 0.1f)
                                                )
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isSelected) Color.Black else Color.White)
                                                .clickable {
                                                    sortOrder = option
                                                    showSortMenu = false
                                                }
                                                .padding(horizontal = 20.dp, vertical = 16.dp)
                                        ) {
                                            Text(
                                                text = option,
                                                color = if (isSelected) Color.White else Color.Black,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        
                        IconCircleButton(
                            icon = { Icon(Icons.Default.Search, "搜索", tint = if (showSearch) Color.White else Color.Black) },
                            isActive = showSearch,
                            onClick = { 
                                showSearch = !showSearch
                                if (!showSearch) {
                                    searchQuery = ""
                                    activeQuickFilter = null
                                }
                                if (showSearch) showCategory = false
                            }
                        )
                    }
                    
                    AnimatedVisibility(visible = showCategory) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("全部", "奶茶", "果茶", "咖啡", "柠檬茶", "纯茶", "瓶装", "酒类", "酸奶").forEach { cat ->
                                FilterChipCustom(
                                    text = cat,
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = showSearch) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("搜索品牌/品项/加料", color = Color.Gray) },
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Black) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        Icon(
                                            Icons.Default.Close, "清空",
                                            modifier = Modifier.clickable { searchQuery = "" }
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("无限复购", "心头好", "本月", "本周").forEach { filter ->
                                    FilterChipCustom(
                                        text = filter,
                                        selected = activeQuickFilter == filter,
                                        onClick = { 
                                            activeQuickFilter = if (activeQuickFilter == filter) null else filter 
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (processedItems.isEmpty()) {
                item { EmptyHint(onRecord = onRecord) }
            } else {
                items(processedItems, key = { it.id }) { tea ->
                    TeaRow(
                        tea = tea,
                        onClick = { onOpenDetail(tea) },
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun IconCircleButton(
    icon: @Composable () -> Unit,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isActive) Color.Black else Color.Transparent)
            .border(1.dp, if (isActive) Color.Black else Color.LightGray, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun FilterChipCustom(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color.Black else Color.Transparent)
            .border(1.dp, if (selected) Color.Black else Color.LightGray, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else Color.Gray
        )
    }
}

@Composable
private fun HeroCard(
    monthCount: Int,
    monthSpend: Double,
    todayStyle: DrinkCategory,
    modifier: Modifier = Modifier,
) {
    val liquid = when {
        monthCount == 0 -> Color(0xFFD9C3AE)
        monthCount == 1 -> Caramel
        monthCount == 2 -> Taro
        else -> Strawberry
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = MaterialTheme.shapes.extraLarge,
                ambientColor = Caramel.copy(alpha = 0.55f),
                spotColor = Caramel.copy(alpha = 0.55f),
            )
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFEFBE93), Color(0xFFD89B69), Color(0xFFC0824F)),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                )
            ),
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .align(Alignment.TopEnd)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)), CircleShape)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 6.dp, top = 20.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "本月已喝", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.92f))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedNumber(
                        value = monthCount,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 62.sp),
                        color = Color.White,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "杯",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    MiniStat(label = "本月花费", value = "¥${formatMoney(monthSpend)}", modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    MiniStat(
                        label = "平均花费",
                        value = "¥${formatMoney(if (monthCount > 0) monthSpend / monthCount else 0.0)}",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            DrinkCup(
                modifier = Modifier.width(104.dp).height(132.dp),
                liquidColor = liquid,
                strawColor = if (monthCount == 0) Color(0xFFB9A896) else Strawberry,
                toppingCount = (3 + monthCount * 2).coerceAtMost(10),
                style = cupStyleFor(todayStyle),
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.20f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.78f))
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickActions(onRecord: () -> Unit, onAiRecord: () -> Unit, onOpenWall: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        AiBanner(onClick = onAiRecord)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            ActionTile(
                title = "手动记",
                subtitle = "自己填更准",
                emoji = "✍️",
                colors = listOf(Caramel, Color(0xFFE2A776)),
                onClick = onRecord,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            ActionTile(
                title = "饮品墙",
                subtitle = "看看攒了多少",
                emoji = "🧱",
                colors = listOf(Taro, Color(0xFFCBBAF5)),
                onClick = onOpenWall,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AiBanner(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "ai")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "aiShift",
    )

    BouncyBox(onClick = onClick) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 14.dp, shape = MaterialTheme.shapes.large, ambientColor = Taro.copy(alpha = 0.55f), spotColor = Taro.copy(alpha = 0.55f))
                .clip(MaterialTheme.shapes.large)
                .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF8E7CF0), Color(0xFFB79CED)))),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                            start = Offset(shift * 600f - 300f, 0f),
                            end = Offset(shift * 600f, 400f),
                        )
                    )
            )
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "🤖", fontSize = 23.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "AI 识图 · 拍一下就好", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = "小票 / 杯身 / 订单截图，自动填好", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.86f))
                }
                Text(text = "→", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActionTile(title: String, subtitle: String, emoji: String, colors: List<Color>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BouncyBox(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 12.dp, shape = MaterialTheme.shapes.large, ambientColor = colors.first().copy(alpha = 0.45f), spotColor = colors.first().copy(alpha = 0.45f))
                .clip(MaterialTheme.shapes.large)
                .background(Brush.linearGradient(colors))
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Column {
                Text(text = emoji, fontSize = 22.sp)
                Spacer(Modifier.height(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun TeaRow(tea: Drink, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val brand = Catalog.brandOf(tea.brand)
    val displayName = tea.name.ifBlank { 
        if (tea.category == "咖啡") "神秘咖啡" else "神秘${tea.category}" 
    }

    BouncyBox(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(68.dp)) {
                    if (tea.photoPath != null) {
                        LocalPhoto(
                            file = PhotoStore.fileOf(tea.photoPath),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = getDrinkIconRes(tea)),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    BrandBadge(
                        badge = brand.badge,
                        color = Color(brand.color),
                        size = 22.dp,
                        logo = brand.logo,
                        squircle = Catalog.isSquircle(brand.name),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFF4F5F0))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = TimeUtil.dayLabel(tea.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = PearlSoft
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tea.brand.ifBlank { "未知品牌" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = PearlSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(Modifier.height(6.dp))
                    RatingStars(rating = tea.rating, starSize = 14.dp)
                    
                    val tags = mutableListOf<String>()
                    if (tea.sugar.isNotBlank()) tags.add(tea.sugar)
                    if (tea.ice.isNotBlank()) tags.add(tea.ice)
                    if (tea.toppings.isNotEmpty()) tags.addAll(tea.toppings)

                    if (tags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .border(0.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(50))
                                        .background(Color.White)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFE0E0E0)))
        }
    }
}

@Composable
private fun EmptyHint(onRecord: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DrinkCup(
            modifier = Modifier.width(120.dp).height(150.dp), 
            liquidColor = Color(0xFFDCC7B4), 
            strawColor = Color(0xFFC9B5A3), 
            toppingCount = 3,
            style = CupStyle.Regular // 👈 这里加上了
        )
        Spacer(Modifier.height(14.dp))
        Text(text = "还没有记录哦", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(text = "喝下的每一杯，都值得被记住", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun formatMoney(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.CHINA, "%.1f", value)

fun getDrinkIconRes(tea: Drink): Int {
    val category = DrinkCategory.of(tea.category)
    val isHot = tea.ice.contains("热") || tea.ice.contains("常温")

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
