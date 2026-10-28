package com.naicha.diary.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.naicha.diary.R
import com.naicha.diary.data.Drink
import com.naicha.diary.data.DrinkCategory
import com.naicha.diary.util.PhotoStore

@Composable
fun DrinkStickerWall(items: List<Drink>, modifier: Modifier = Modifier) {
    // 取最近的 8 杯展示
    val displayItems = remember(items) { items.takeLast(8) }
    
    if (displayItems.isEmpty()) return

    // 外层包一个大白底圆角卡片
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(vertical = 24.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            displayItems.forEachIndexed { index, tea ->
                // 制造错落的 Y 轴偏移
                val yOffset = when (index % 4) {
                    0 -> (-10).dp
                    1 -> 10.dp
                    2 -> (-5).dp
                    else -> 5.dp
                }
                // 制造轻微的旋转角度
                val rotation = when (index % 4) {
                    0 -> -6f
                    1 -> 8f
                    2 -> -4f
                    else -> 5f
                }
                Box(
                    modifier = Modifier
                        .offset(y = yOffset)
                        .rotate(rotation)
                        .padding(horizontal = 2.dp)
                ) {
                    StickerDrinkView(tea = tea, modifier = Modifier.size(64.dp))
                }
            }
        }
    }
}

@Composable
fun StickerDrinkView(tea: Drink, modifier: Modifier = Modifier) {
    // 获取图片资源
    val defaultBitmap = if (tea.photoPath == null) ImageBitmap.imageResource(id = getDrinkIconRes(tea)) else null
    val customBitmap = remember(tea) {
        if (tea.photoPath != null) {
            val file = PhotoStore.fileOf(tea.photoPath)
            val raw = BitmapFactory.decodeFile(file.absolutePath)
            if (raw != null) trimTransparentEdges(raw).asImageBitmap() else null
        } else null
    }

    val imageBitmap = customBitmap ?: defaultBitmap

    if (imageBitmap != null) {
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
            
            // 白边厚度 2dp
            val strokePx = 2.dp.toPx().toInt()
            
            val offsets = listOf(
                IntOffset(-strokePx, -strokePx), IntOffset(0, -strokePx), IntOffset(strokePx, -strokePx),
                IntOffset(-strokePx, 0), IntOffset(strokePx, 0),
                IntOffset(-strokePx, strokePx), IntOffset(0, strokePx), IntOffset(strokePx, strokePx)
            )
            
            // 画 8 个方向的纯白底影
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
            
            // 叠原图
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
