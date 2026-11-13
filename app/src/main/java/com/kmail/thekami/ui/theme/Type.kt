package com.kmail.thekami.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

// System font with Noto fallback covers all supported scripts.
// Letter spacing stays 0: positive spacing breaks Arabic letter joining.
private val Base = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

internal val KmailTypography = Typography(
    displayLarge = Base.copy(fontSize = 57.sp, lineHeight = 72.sp),
    displayMedium = Base.copy(fontSize = 45.sp, lineHeight = 58.sp),
    displaySmall = Base.copy(fontSize = 36.sp, lineHeight = 48.sp),
    headlineLarge = Base.copy(fontSize = 32.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = Base.copy(fontSize = 28.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = Base.copy(fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = Base.copy(fontSize = 22.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.copy(fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
    titleSmall = Base.copy(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = Base.copy(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = Base.copy(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = Base.copy(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = Base.copy(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    labelMedium = Base.copy(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelSmall = Base.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)
