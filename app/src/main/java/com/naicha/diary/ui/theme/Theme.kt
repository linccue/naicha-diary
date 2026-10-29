package com.naicha.diary.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.naicha.diary.R

private val DrinkColors = lightColorScheme(
    primary = Caramel,
    onPrimary = Color.White,
    primaryContainer = CaramelLight,
    onPrimaryContainer = Color(0xFF5C3A1E),
    secondary = Taro,
    onSecondary = Color.White,
    secondaryContainer = TaroLight,
    onSecondaryContainer = Color(0xFF3B2A63),
    tertiary = Matcha,
    onTertiary = Color.White,
    tertiaryContainer = MatchaLight,
    onTertiaryContainer = Color(0xFF26401F),
    background = Color(0xFFF4F5F0),
    onBackground = Pearl,
    surface = Color(0xFFF4F5F0),
    onSurface = Pearl,
    surfaceVariant = CreamDeep,
    onSurfaceVariant = PearlSoft,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFCF8),
    surfaceContainer = Color(0xFFFDF5EC),
    surfaceContainerHigh = Color(0xFFF9EFE3),
    surfaceContainerHighest = Color(0xFFF4E8DA),
    outline = Outline,
    outlineVariant = Color(0xFFF2E6D8),
    error = Color(0xFFE06C75),
    onError = Color.White,
    scrim = Color(0x66000000),
)

// 👇 1. 定义你的 Zcool 字体
val ZcoolFontFamily = FontFamily(Font(R.font.zcool))

// 👇 2. 将原本的排版配置全部替换为 Zcool 字体
val ZcoolTypography = Typography(
    displayLarge = DrinkTypography.displayLarge.copy(fontFamily = ZcoolFontFamily),
    displayMedium = DrinkTypography.displayMedium.copy(fontFamily = ZcoolFontFamily),
    displaySmall = DrinkTypography.displaySmall.copy(fontFamily = ZcoolFontFamily),
    headlineLarge = DrinkTypography.headlineLarge.copy(fontFamily = ZcoolFontFamily),
    headlineMedium = DrinkTypography.headlineMedium.copy(fontFamily = ZcoolFontFamily),
    headlineSmall = DrinkTypography.headlineSmall.copy(fontFamily = ZcoolFontFamily),
    titleLarge = DrinkTypography.titleLarge.copy(fontFamily = ZcoolFontFamily),
    titleMedium = DrinkTypography.titleMedium.copy(fontFamily = ZcoolFontFamily),
    titleSmall = DrinkTypography.titleSmall.copy(fontFamily = ZcoolFontFamily),
    bodyLarge = DrinkTypography.bodyLarge.copy(fontFamily = ZcoolFontFamily),
    bodyMedium = DrinkTypography.bodyMedium.copy(fontFamily = ZcoolFontFamily),
    bodySmall = DrinkTypography.bodySmall.copy(fontFamily = ZcoolFontFamily),
    labelLarge = DrinkTypography.labelLarge.copy(fontFamily = ZcoolFontFamily),
    labelMedium = DrinkTypography.labelMedium.copy(fontFamily = ZcoolFontFamily),
    labelSmall = DrinkTypography.labelSmall.copy(fontFamily = ZcoolFontFamily)
)

@Composable
fun DrinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DrinkColors,
        typography = ZcoolTypography, // 👈 3. 替换为新的 Zcool 字体排版
        shapes = DrinkShapes,
        content = content,
    )
}
