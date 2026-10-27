package com.naicha.diary.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.naicha.diary.data.DrinkCategory

enum class CupStyle { Regular, Tall, Short }

fun cupStyleFor(category: DrinkCategory): CupStyle {
    return when (category) {
        DrinkCategory.Coffee, DrinkCategory.PureTea, DrinkCategory.Bottled -> CupStyle.Tall
        DrinkCategory.Other -> CupStyle.Short
        else -> CupStyle.Regular
    }
}

@Composable
fun DrinkCup(
    liquidColor: Color,
    strawColor: Color,
    toppingCount: Int,
    style: CupStyle,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeWidth = w * 0.06f

            // 杯身轮廓
            val cupPath = Path().apply {
                moveTo(w * 0.2f, h * 0.1f)
                lineTo(w * 0.8f, h * 0.1f)
                lineTo(w * 0.9f, h * 0.9f)
                lineTo(w * 0.1f, h * 0.9f)
                close()
            }
            drawPath(cupPath, color = Color.Black, style = Stroke(strokeWidth))

            // 杯盖
            drawRect(
                color = Color.Black,
                topLeft = Offset(w * 0.15f, h * 0.05f),
                size = Size(w * 0.7f, h * 0.1f)
            )

            // 液体
            val liquidPath = Path().apply {
                moveTo(w * 0.22f, h * 0.4f)
                lineTo(w * 0.78f, h * 0.4f)
                lineTo(w * 0.87f, h * 0.88f)
                lineTo(w * 0.13f, h * 0.88f)
                close()
            }
            drawPath(liquidPath, color = liquidColor)

            // 珍珠
            repeat(toppingCount.coerceAtMost(10)) { i ->
                val px = w * (0.2f + (i % 4) * 0.15f)
                val py = h * (0.75f + (i / 4) * 0.08f)
                drawCircle(Color(0xFF3E2723), radius = w * 0.04f, center = Offset(px, py))
            }

            // 吸管
            drawLine(
                color = strawColor,
                start = Offset(w * 0.5f, h * 0.1f),
                end = Offset(w * 0.3f, h * 0.5f),
                strokeWidth = strokeWidth * 1.5f
            )
        }
    }
}
