package com.naicha.diary.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.ContentScale
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WallMode(val label: String, val emoji: String) {
    Wall("饮品墙", "🧱"),
    Brand("品牌榜", "🏆"),
    Timeline("时间轴", "🕰"),
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

    Box(modifier.fillMaxSize()) {
        PearlField(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF4F5F0)),
            count = 16,
            seed = 11,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ======== 顶部标题与切换标签（保留原样） ========
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

            // ======== 内容区域 ========
            when (mode) {
                WallMode.Wall -> StickerWallView(items = sorted, onOpenDetail = onOpenDetail)
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

// ================= 改造后的饮品墙：贴纸墙 + 日期分组 =================
@Composable
private fun StickerWallView(items: List<Drink>, onOpenDetail: (Drink) -> Unit) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "墙上还空空的哦", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // 按日期分组
    val groupedItems = remember(items) {
        items.groupBy { SimpleDateFormat("dd日 EEEE", Locale.CHINA).format(Date(it.timestamp)) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 顶部贴纸墙
        item {
            StickerDisplayWall(items)
        }

        // 2. 按日期分组的列表
        groupedItems.forEach { (dateStr, dayItems) ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateStr,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "${dayItems.size} 杯",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PearlSoft,
                        modifier = Modifier.background(Color.White, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            items(dayItems, key = { it.id }) { tea ->
                WallStickerRow(tea = tea, onClick = { onOpenDetail(tea) })
            }
        }
    }
}

// 顶部错落贴纸墙
@Composable
private fun StickerDisplayWall(items: List<Drink>) {
    val displayItems = remember(items) { items.take(8) }
    if (displayItems.isEmpty()) return

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFFE6E8E0)).padding(vertical = 32.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            displayItems.forEachIndexed { index, tea ->
                val yOffset = when (index % 4) { 0 -> (-12).dp; 1 -> 12.dp; 2 -> (-4).dp; else -> 6.dp }
                val rotation = when (index % 4) { 0 -> -8f; 1 -> 10f; 2 -> -3f; else -> 5f }
                Box(modifier = Modifier.offset(y = yOffset).rotate(rotation).padding(horizontal = 2.dp)) {
                    StickerImage(tea = tea, modifier = Modifier.size(64.dp))
                }
            }
        }
    }
}

// 列表项
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

// ================= 核心白边贴纸组件 =================
@Composable
fun StickerImage(tea: Drink, modifier: Modifier = Modifier) {
    val defaultBitmap = if (tea.photoPath == null) ImageBitmap.imageResource(id = getDrinkIconRes(tea)) else null
    val customBitmap = remember(tea) {
        if (tea.photoPath != null) {
            val file = PhotoStore.fileOf(tea.photoPath)
            val raw = BitmapFactory.decodeFile(file.absolutePath)
            if (raw != null) trimTransparentEdges(raw).asImageBitmap() else null
        } else null
    }

    val imageBitmap = customBitmap ?: defaultBitmap
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

// 裁剪透明边缘
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

// 预设图片匹配
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

// ================= 原有保留组件（品牌榜、时间轴） =================

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

fun formatMoney(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.CHINA, "%.1f", value)
