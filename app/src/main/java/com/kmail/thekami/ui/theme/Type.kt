package com.kmail.thekami.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.kmail.thekami.R

// Inter for Latin text. Other scripts fall back to the system fonts, so they still render correctly.
// Letter spacing stays 0: positive spacing breaks Arabic letter joining.
private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val Base = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

internal val KmailTypography = Typography(
    displayLarge = Base.copy(fontSize = 52.sp, lineHeight = 64.sp, fontWeight = FontWeight.SemiBold),
    displayMedium = Base.copy(fontSize = 42.sp, lineHeight = 52.sp, fontWeight = FontWeight.SemiBold),
    displaySmall = Base.copy(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold),
    headlineLarge = Base.copy(fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = Base.copy(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = Base.copy(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = Base.copy(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall = Base.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = Base.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = Base.copy(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = Base.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = Base.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = Base.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)
