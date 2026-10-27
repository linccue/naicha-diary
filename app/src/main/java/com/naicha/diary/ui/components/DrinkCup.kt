package com.naicha.diary.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.naicha.diary.data.DrinkCategory

enum class CupStyle { Regular, Tall, Short }

fun cupStyleFor(category: DrinkCategory): CupStyle {
    return when (category) {
        DrinkCategory.Coffee, DrinkCategory.PureTea, DrinkCategory.Bottled -> CupStyle.Tall
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
            val strokeW = w * 0.06f
            val blackColor = Color(0xFF1C1C1E)

            val cupW = if (style == CupStyle.Tall) w * 0.5f else w * 0.65f
            val cupH = if (style == CupStyle.Short) h * 0.5f else h * 0.65f
            val startX = (w - cupW) / 2f
            val startY = h * 0.25f

            // 1. 吸管
            val strawPath = Path().apply {
                moveTo(startX + cupW * 0.5f, startY + cupH * 0.3f)
                lineTo(startX + cupW * 0.8f, h * 0.05f)
            }
            drawPath(
                path = strawPath,
                color = strawColor,
                style = Stroke(width = strokeW * 2f, cap = StrokeCap.Round)
            )

            // 2. 液体
            val liquidPath = Path().apply {
                moveTo(startX + cupW * 0.05f, startY + cupH * 0.35f)
                lineTo(startX + cupW * 0.95f, startY + cupH * 0.35f)
                lineTo(startX + cupW * 0.85f, startY + cupH)
                lineTo(startX + cupW * 0.15f, startY + cupH)
                close()
            }
            drawPath(path = liquidPath, color = liquidColor)

            // 3. 珍珠
            val pearlCount = toppingCount.coerceIn(1, 6)
            for (i in 0 until pearlCount) {
                val px = startX + cupW * (0.25f + (i % 3) * 0.25f)
                val py = startY + cupH * (0.75f + (i / 3) * 0.15f)
                drawCircle(color = blackColor.copy(alpha = 0.7f), radius = w * 0.04f, center = Offset(px, py))
            }

            // 4. 杯身轮廓
            val cupOutlinePath = Path().apply {
                moveTo(startX, startY)
                lineTo(startX + cupW, startY)
                lineTo(startX + cupW * 0.85f, startY + cupH - w * 0.05f)
                quadraticBezierTo(startX + cupW * 0.85f, startY + cupH, startX + cupW * 0.85f - w * 0.05f, startY + cupH)
                lineTo(startX + cupW * 0.15f + w * 0.05f, startY + cupH)
                quadraticBezierTo(startX + cupW * 0.15f, startY + cupH, startX + cupW * 0.15f, startY + cupH - w * 0.05f)
                close()
            }
            drawPath(
                path = cupOutlinePath,
                color = blackColor,
                style = Stroke(width = strokeW, join = StrokeJoin.Round)
            )

            // 5. 杯盖
            val lidW = cupW * 1.1f
            val lidH = h * 0.12f
            val lidX = startX - cupW * 0.05f
            val lidY = startY - lidH * 0.8f
            drawRoundRect(
                color = blackColor,
                topLeft = Offset(lidX, lidY),
                size = Size(lidW, lidH),
                cornerRadius = CornerRadius(lidH / 2, lidH / 2)
            )
            drawRoundRect(
                color = blackColor,
                topLeft = Offset(lidX + lidW * 0.15f, lidY - lidH * 0.4f),
                size = Size(lidW * 0.7f, lidH * 0.8f),
                cornerRadius = CornerRadius(lidH / 2, lidH / 2)
            )
            
            // 高光
            val highlightPath = Path().apply {
                moveTo(startX + cupW * 0.2f, startY + cupH * 0.45f)
                lineTo(startX + cupW * 0.2f, startY + cupH * 0.65f)
            }
            drawPath(
                path = highlightPath,
                color = Color.White.copy(alpha = 0.6f),
                style = Stroke(width = strokeW * 1.5f, cap = StrokeCap.Round)
            )
        }
    }
}
