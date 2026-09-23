package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QrGeneratorHelper {

    /**
     * Generates a real scannable QR Code Bitmap using ZXing QRCodeWriter.
     */
    fun generateQrBitmap(
        content: String,
        width: Int = 512,
        height: Int = 512,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
            }

            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            )

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) darkColor else lightColor)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            // Retry with ErrorCorrectionLevel.L and zero margin if payload is large
            try {
                val fallbackHints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                    put(EncodeHintType.CHARACTER_SET, "UTF-8")
                    put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L)
                    put(EncodeHintType.MARGIN, 0)
                }
                val bitMatrix = QRCodeWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    fallbackHints
                )
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) darkColor else lightColor)
                    }
                }
                bitmap
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
                null
            }
        }
    }

    /**
     * Generates an ImageBitmap directly for Jetpack Compose.
     */
    fun generateQrImageBitmap(
        content: String,
        size: Int = 512,
        darkColor: Int = Color.parseColor("#0F766E") // Tawthiq teal-dark
    ): ImageBitmap? {
        return generateQrBitmap(content, size, size, darkColor, Color.WHITE)?.asImageBitmap()
    }
}
