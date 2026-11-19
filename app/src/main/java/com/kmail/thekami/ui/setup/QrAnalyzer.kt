package com.kmail.thekami.ui.setup

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer

/** Looks for a QR code in every camera frame. [onText] runs on the analysis thread. */
internal class QrAnalyzer : ImageAnalysis.Analyzer {
    var onText: (String) -> Unit = {}

    @Volatile
    private var stopped = false

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
            ),
        )
    }

    fun stop() {
        stopped = true
    }

    override fun analyze(image: ImageProxy) {
        try {
            if (stopped) return
            val plane = image.planes[0]
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val source = PlanarYUVLuminanceSource(
                bytes, plane.rowStride, image.height, 0, 0, image.width, image.height, false,
            )
            onText(reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text)
        } catch (e: ReaderException) {
            // No QR code in this frame.
        } finally {
            reader.reset()
            image.close()
        }
    }
}
