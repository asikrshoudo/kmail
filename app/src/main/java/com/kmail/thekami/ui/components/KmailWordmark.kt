package com.kmail.thekami.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.kmail.thekami.R

/** "Kmail" with the K in the brand teal. The teal is lighter on dark backgrounds so it stays readable. */
@Composable
internal fun KmailWordmark(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
) {
    val name = stringResource(R.string.app_name)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val teal = if (dark) Color(0xFF4DB6AC) else Color(0xFF00897B)
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = teal)) { append(name.take(1)) }
        append(name.drop(1))
    }
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    )
}
