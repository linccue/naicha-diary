package com.naicha.diary.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.naicha.diary.data.Catalog
import com.naicha.diary.data.Drink
import com.naicha.diary.ui.components.*
import com.naicha.diary.ui.theme.*
import com.naicha.diary.util.PhotoStore
import com.naicha.diary.util.TimeUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class WallMode(val label: String, val emoji: String) {
    Wall("饮品墙", "🧱"),
    Brand("品牌榜", "🏆"),
    Timeline("时间轴", "🕰"),
}

// ================= 内存缓存，解决滑动卡顿 =================
object StickerImageCache {
    private val cache = mutableMapOf<String, ImageBitmap?>()
    
    fun getImage(tea: Drink): ImageBitmap? {
        val key = tea.photoPath ?: "preset_${getDrinkIconRes(tea)}"
        if (cache.containsKey(key)) return cache[key]

        val bitmap = if (tea.photoPath == null) {
            null // 预设图通过 ImageBitmap.imageResource 加载，无需文件解析
        } else {
            val file = PhotoStore.fileOf(tea.photoPath)
            val raw = BitmapFactory.decodeFile(file.absolutePath)
            if (raw != null) trimTransparentEdges(raw).asImageBitmap() else null
        }
        cache[key] = bitmap
        return bitmap
    }
}

// 贴纸状态类
class StickerNode(
    val tea: Drink,
    val initialX: Float,
    val initialY: Float,
    val rotation: Float
) {
    var offsetX by mutableStateOf(initialX)
    var offsetY by mutableStateOf(initialY)
    var isDragging by mutableStateOf(false)
    var hasAnimated by mutableStateOf(false) // 记录是否播放过动画
}

@Composable
fun WallScreen(
    items: List<Drink>,
    onOpenDetail: (Drink) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(WallMode.Wall) }
    var showPoster by remember { mutableStateOf(false) }

    val sorted = remember(items) { items.sortedByDescending { it.timestamp } }
    val totalSpend = remember(items) { items.sumOf { it.price } }
    val topBrand = remember(items) {
        items.groupingBy { it.brand }.eachCount().maxByOrNull { it.value }?.key
    }
    val photoCount = remember(items) { items.count { it.photoPath != null } }

    // 👇 将贴纸节点状态提升到最外层，保证滑动时不丢失、不重播
    val displayItems = remember(sorted) { sorted.take(8) }
    val density = LocalDensity.current
    val cardSize = 64.dp
    val cardSizePx = with(density) { cardSize.toPx() }

    // 使用固定的网格生成算法，保证每次重组得到的初始位置不变
    val nodes = remember(displayItems) {
        val list = mutableListOf<StickerNode>()
        val cols = 3
        val rows = 2
        val cellWidth = 350f / cols // 假设屏宽大概 350dp
        val cellHeight = 220f / rows // 框高 220dp
        
        val shuffledCells = (0 until (cols * rows)).shuffled()
        
        displayItems.forEachIndexed { index, tea ->
            val cellIndex = shuffledCells[index % (cols * rows)]
            val col = cellIndex % cols
            val row = cellIndex / cols
            
            // 使用随机种子保证同一个贴纸每次计算出的位置抖动是固定的
            val random = Random(tea.id.hashCode())
            val jitterX = (random.nextFloat() - 0.5f) * cellWidth * 0.4f
            val jitterY = (random.nextFloat() - 0.5f) * cellHeight * 0.4f
            
            val x = col * cellWidth + cellWidth / 2f - cardSizePx / 2f + jitterX
            val y = row * cellHeight + cellHeight / 2f - cardSizePx / 2f + jitterY

            list.add(StickerNode(tea, x, y, random.nextFloat() * 20f - 10f))
        }
        list
    }

    Box(modifier.fillMaxSize()) {
        PearlField(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF4F5F0)),
            count = 16,
            seed = 11,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "我的饮品墙 🧱",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = if (items.isEmpty()) "还没有故事" else
                                "共 ${items.size} 杯 · ¥${formatMoney(totalSpend)}" +
                                    (topBrand?.let { " · 最爱 $it" } ?: ""),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BouncyBox(onClick = { if (items.isNotEmpty()) showPoster = true }) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(8.dp, CircleShape, ambientColor = Caramel.copy(alpha = 0.5f), spotColor = Caramel.copy(alpha = 0.5f))
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Caramel, Color(0xFFE2A776)))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Share, "生成分享图", tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WallMode.entries.forEach { entry ->
                        TagChip(
                            text = "${entry.emoji} ${entry.label}",
                            selected = mode == entry,
                            accent = Caramel,
                            onClick = { mode = entry },
                        )
                    }
                }
            }

            when (mode) {
                WallMode.Wall -> WallTabContent(
                    items = sorted, 
                    nodes = nodes, // 传入提升的节点
                    onOpenDetail = onOpenDetail
                )
                WallMode.Brand -> BrandRank(items = sorted)
                WallMode.Timeline -> TimelineView(items = sorted, onOpenDetail = onOpenDetail)
            }
        }

        if (showPoster) {
            SharePosterDialog(
                items = sorted,
                totalSpend = totalSpend,
                topBrand = topBrand,
                photoCount = photoCount,
                onDismiss = { showPoster = false },
            )
        }
    }
}

@Composable
private fun WallTabContent(
    items: List<Drink>, 
    nodes: List<StickerNode>,
    onOpenDetail: (Drink) -> Unit
) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDay by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    val year = currentMonth.get(Calendar.YEAR)
    val month = currentMonth.get(Calendar.MONTH)
    
    val monthItems = remember(items, year, month) {
        items.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
        }
    }
    
    val dayRecords = remember(monthItems) {
        monthItems.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.DAY_OF_MONTH)
        }
    }

    val selectedDayRecords = dayRecords[selectedDay] ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 贴纸墙
        item {
            DraggableStickerWall(nodes)
        }

        // 2. 日历
        item {
            CalendarCard(
                currentMonth = currentMonth,
                onMonthChange = { 
                    currentMonth = it 
                    selectedDay = 1 
                },
                monthItems = monthItems,
                dayRecords = dayRecords,
                selectedDay = selectedDay,
                onSelectDay = { selectedDay = it }
            )
        }

        // 3. 当天记录
        item {
            val dateStr = SimpleDateFormat("d日 EEEE", Locale.CHINA).format(
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }.time
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = dateStr, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(
                    text = "${selectedDayRecords.size} 杯", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PearlSoft,
                    modifier = Modifier.background(Color(0xFFF4F5F0), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (selectedDayRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFF4F5F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "当天暂无记录", style = MaterialTheme.typography.bodyMedium, color = PearlSoft)
                }
            }
        } else {
            items(selectedDayRecords, key = { it.id }) { tea ->
                WallStickerRow(tea = tea, onClick = { onOpenDetail(tea) })
            }
        }
    }
}

@Composable
private fun DraggableStickerWall(nodes: List<StickerNode>) {
    if (nodes.isEmpty()) return

    val cardSize = 64.dp
    val wallHeight = 220.dp
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(wallHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFE6E8E0))
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()
        val cardSizePx = with(density) { cardSize.toPx() }

        nodes.forEachIndexed { index, node ->
            DraggableStickerItem(
                node = node,
                index = index,
                cardSize = cardSize,
                maxX = maxWidthPx - cardSizePx,
                maxY = maxHeightPx - cardSizePx
            )
        }
    }
}

@Composable
private fun DraggableStickerItem(
    node: StickerNode,
    index: Int,
    cardSize: Dp,
    maxX: Float,
    maxY: Float
) {
    // 如果已经动画过了，初始值直接设为最终位置，不再从上往下掉
    val animY = remember { Animatable(if (node.hasAnimated) node.offsetY else -400f) }
    
    LaunchedEffect(Unit) {
        if (!node.hasAnimated) {
            kotlinx.coroutines.delay(index * 100L)
            animY.animateTo(
                targetValue = node.offsetY,
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = FastOutSlowInEasing
                )
            )
            node.hasAnimated = true // 标记已播放完毕
        }
    }

    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .zIndex(if (node.isDragging) 10f else 1f)
            .offset {
                // 如果正在播放动画，用动画值；如果播完了，用静态值
                val curX = if (animY.isRunning) node.offsetX else (node.offsetX + dragOffsetX).coerceIn(0f, maxX)
                val curY = if (animY.isRunning) animY.value else (node.offsetY + dragOffsetY).coerceIn(0f, maxY)
                IntOffset(curX.roundToInt(), curY.roundToInt())
            }
            .size(cardSize)
            .rotate(node.rotation)
            .pointerInput(node) {
                detectDragGestures(
                    onDragStart = { node.isDragging = true },
                    onDragEnd = { 
                        node.isDragging = false
                        node.offsetX = (node.offsetX + dragOffsetX).coerceIn(0f, maxX)
                        node.offsetY = (node.offsetY + dragOffsetY).coerceIn(0f, maxY)
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newDragX = dragOffsetX + dragAmount.x
                        val newDragY = dragOffsetY + dragAmount.y
                        dragOffsetX = newDragX.coerceIn(-node.offsetX, maxX - node.offsetX)
                        dragOffsetY = newDragY.coerceIn(-node.offsetY, maxY - node.offsetY)
                    }
                )
            }
    ) {
        StickerImage(tea = node.tea, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun CalendarCard(
    currentMonth: Calendar,
    onMonthChange: (Calendar) -> Unit,
    monthItems: List<Drink>,
    dayRecords: Map<Int, List<Drink>>,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit
) {
    val year = currentMonth.get(Calendar.YEAR)
    val month = currentMonth.get(Calendar.MONTH)
    val totalSpend = monthItems.sumOf { it.price }
    val totalCups = monthItems.size
    val totalBrands = monthItems.map { it.brand }.distinct().size

    val firstDay = currentMonth.clone() as Calendar
    firstDay.set(Calendar.DAY_OF_MONTH, 1)
    val leading = (firstDay.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

    val today = Calendar.getInstance()

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "${year}年${month + 1}月", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row {
                Icon(Icons.Default.KeyboardArrowLeft, "上个月", modifier = Modifier.size(28.dp).clip(CircleShape).clickable { 
                    val c = currentMonth.clone() as Calendar; c.add(Calendar.MONTH, -1); onMonthChange(c) 
                })
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Default.KeyboardArrowRight, "下个月", modifier = Modifier.size(28.dp).clip(CircleShape).clickable { 
                    val c = currentMonth.clone() as Calendar; c.add(Calendar.MONTH, 1); onMonthChange(c) 
                })
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF8F9FA)).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCol("该月消费", "¥${formatMoney(totalSpend)}")
            Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFE0E0E0)))
            StatCol("该月杯数", "${totalCups} 杯")
            Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFE0E0E0)))
            StatCol("该月品牌", "${totalBrands} 个")
        }

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }

        val cells = leading + daysInMonth
        val rows = (cells + 6) / 7
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val index = row * 7 + col
                    val day = index - leading + 1
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(4.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..daysInMonth) {
                            val drinks = dayRecords[day]
                            val isSelected = day == selectedDay
                            val isToday = day == today.get(Calendar.DAY_OF_MONTH) && month == today.get(Calendar.MONTH)

                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFFF0F0F0) else Color.Transparent)
                                    .clickable { onSelectDay(day) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!drinks.isNullOrEmpty()) {
                                    val brand = Catalog.brandOf(drinks.first().brand)
                                    BrandBadge(
                                        badge = brand.badge,
                                        color = Color(brand.color),
                                        size = 32.dp,
                                        logo = brand.logo,
                                        squircle = Catalog.isSquircle(brand.name)
                                    )
                                } else {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isToday) Caramel else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(50)).clickable {
                    val c = Calendar.getInstance()
                    onMonthChange(c)
                    onSelectDay(c.get(Calendar.DAY_OF_MONTH))
                }.padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("回到今天", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            }
        }
    }
}

@Composable
private fun StatCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = PearlSoft)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
private fun WallStickerRow(tea: Drink, onClick: () -> Unit) {
    val brand = Catalog.brandOf(tea.brand)
    val displayName = tea.name.ifBlank { if (tea.category == "咖啡") "神秘咖啡" else "神秘${tea.category}" }

    BouncyBox(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                StickerImage(tea = tea, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).background(Color.White, CircleShape).padding(2.dp)) {
                    BrandBadge(badge = brand.badge, color = Color(brand.color), size = 20.dp, logo = brand.logo, squircle = Catalog.isSquircle(brand.name))
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = displayName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                Text(text = tea.brand.ifBlank { "未知品牌" }, style = MaterialTheme.typography.bodyMedium, color = PearlSoft)
                Spacer(Modifier.height(6.dp))
                RatingStars(rating = tea.rating, starSize = 14.dp)
                
                val tags = mutableListOf<String>()
                if (tea.sugar.isNotBlank()) tags.add(tea.sugar)
                if (tea.ice.isNotBlank()) tags.add(tea.ice)
                if (tea.toppings.isNotEmpty()) tags.addAll(tea.toppings)

                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tags.forEach { tag ->
                            Box(modifier = Modifier.clip(RoundedCornerShape(50)).border(0.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(50)).background(Color.White).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(text = tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StickerImage(tea: Drink, modifier: Modifier = Modifier) {
    // 👇 使用内存缓存，彻底解决滑动卡顿
    val imageBitmap = remember(tea) {
        if (tea.photoPath == null) ImageBitmap.imageResource(id = getDrinkIconRes(tea)) 
        else StickerImageCache.getImage(tea)
    }

    if (imageBitmap == null) return

    Canvas(modifier = modifier) {
        val imgWidth = imageBitmap.width.toFloat()
        val imgHeight = imageBitmap.height.toFloat()
        val canvasRatio = size.width / size.height
        val imgRatio = imgWidth / imgHeight
        
        val drawWidth: Float
        val drawHeight: Float
        if (canvasRatio > imgRatio) {
            drawHeight = size.height
            drawWidth = drawHeight * imgRatio
        } else {
            drawWidth = size.width
            drawHeight = drawWidth / imgRatio
        }
        
        val dx = (size.width - drawWidth) / 2f
        val dy = (size.height - drawHeight) / 2f
        val dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
        val dstOffset = IntOffset(dx.toInt(), dy.toInt())
        
        val strokePx = 2.dp.toPx().toInt()
        val offsets = listOf(
            IntOffset(-strokePx, -strokePx), IntOffset(0, -strokePx), IntOffset(strokePx, -strokePx),
            IntOffset(-strokePx, 0), IntOffset(strokePx, 0),
            IntOffset(-strokePx, strokePx), IntOffset(0, strokePx), IntOffset(strokePx, strokePx)
        )
        
        offsets.forEach { offset ->
            drawImage(
                image = imageBitmap,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(imageBitmap.width, imageBitmap.height),
                dstOffset = IntOffset(dstOffset.x + offset.x, dstOffset.y + offset.y),
                dstSize = dstSize,
                colorFilter = ColorFilter.tint(Color.White),
                filterQuality = FilterQuality.High
            )
        }
        
        drawImage(
            image = imageBitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(imageBitmap.width, imageBitmap.height),
            dstOffset = dstOffset,
            dstSize = dstSize,
            colorFilter = null,
            filterQuality = FilterQuality.High
        )
    }
}

@Composable
private fun BrandRank(items: List<Drink>) {
    val ranking = remember(items) {
        items.groupBy { it.brand }
            .map { (brand, list) -> Triple(brand, list.size, list.sumOf { it.price }) }
            .sortedByDescending { it.second }
    }
    val max = ranking.firstOrNull()?.second ?: 1

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(ranking, key = { it.first }) { (brandName, count, spend) ->
            val brand = Catalog.brandOf(brandName)
            val color = Color(brand.color)
            SoftCard(elevation = 4.dp, shadowColor = color) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandBadge(badge = brand.badge, color = color, size = 46.dp, logo = brand.logo, squircle = Catalog.isSquircle(brand.name))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = brandName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.weight(1f))
                            Text(text = "$count 杯", style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(7.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape).background(color.copy(alpha = 0.14f))) {
                            Box(modifier = Modifier.fillMaxWidth((count.toFloat() / max).coerceIn(0.06f, 1f)).height(9.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f)))))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(text = "累计 ¥${formatMoney(spend)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineView(items: List<Drink>, onOpenDetail: (Drink) -> Unit) {
    val grouped = remember(items) {
        items.groupBy { TimeUtil.monthKey(it.timestamp) }.toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        grouped.forEach { (month, teas) ->
            item(key = "header_$month") {
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = month.replace("-", " 年 ") + " 月", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.clip(CircleShape).background(Caramel.copy(alpha = 0.15f)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                        Text(text = "${teas.size} 杯", style = MaterialTheme.typography.labelMedium, color = Caramel)
                    }
                }
            }
            items(teas, key = { it.id }) { tea ->
                TimelineRow(tea = tea, onClick = { onOpenDetail(tea) })
            }
        }
    }
}

@Composable
private fun TimelineRow(tea: Drink, onClick: () -> Unit) {
    val brand = Catalog.brandOf(tea.brand)
    val color = Color(brand.color)
    BouncyBox(onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.width(3.dp).height(12.dp).clip(CircleShape).background(color.copy(alpha = 0.35f)))
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                Box(modifier = Modifier.width(3.dp).height(12.dp).clip(CircleShape).background(color.copy(alpha = 0.35f)))
            }
            Spacer(Modifier.width(12.dp))
            if (tea.photoPath != null) {
                LocalPhoto(file = PhotoStore.fileOf(tea.photoPath), modifier = Modifier.size(54.dp).clip(MaterialTheme.shapes.medium), maxSize = 320)
                Spacer(Modifier.width(12.dp))
            } else {
                BrandBadge(badge = brand.badge, color = color, size = 40.dp, logo = brand.logo, squircle = Catalog.isSquircle(brand.name))
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tea.name.ifBlank { "一杯${tea.brand}" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(text = "${tea.brand} · ${tea.cupSize} · ${tea.sugar}", style = MaterialTheme.typography.labelMedium, color = PearlSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingStars(rating = tea.rating, starSize = 11.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(text = TimeUtil.fullLabel(tea.timestamp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (tea.price > 0) {
                Text(text = "¥${formatMoney(tea.price)}", style = MaterialTheme.typography.titleMedium, color = Caramel)
            }
        }
    }
}
