package com.kmail.thekami.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** One cell per QR module, without a quiet zone. */
internal class QrMatrix(val size: Int, private val cells: BooleanArray) {
    fun isDark(x: Int, y: Int): Boolean = cells[y * size + x]
}

/** @return null if [text] does not fit into a QR code. */
internal fun buildQr(text: String): QrMatrix? = try {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
        EncodeHintType.MARGIN to 0,
    )
    val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
    QrMatrix(bits.width, BooleanArray(bits.width * bits.height) { bits.get(it % bits.width, it / bits.width) })
} catch (e: WriterException) {
    null
}

/** Always dark on white, whatever the app theme is, because scanners need the contrast. */
@Composable
internal fun QrImage(matrix: QrMatrix, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1f).background(Color.White)) {
        val quiet = 4
        val cell = size.minDimension / (matrix.size + quiet * 2)
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix.isDark(x, y)) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset((x + quiet) * cell, (y + quiet) * cell),
                        size = Size(cell + 0.5f, cell + 0.5f),
                    )
                }
            }
        }
    }
}
