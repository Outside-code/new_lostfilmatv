package com.kraat.lostfilmnewtv.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeGeneratorTest {

    @Test
    fun generateMatrix_returnsRequestedSize() {
        val matrix = QrCodeGenerator.generateMatrix(
            content = "https://example-phone.auth.example.test/",
            size = 256,
        )

        assertEquals(256, matrix.width)
        assertEquals(256, matrix.height)
        assertNotEquals(matrix.get(0, 0), matrix.get(128, 128))
    }

    @Test
    fun generatedCodeCanBeScannedWithTheCompletePairingUrl() {
        val url = "https://auth.example.com/pair/phone-verifier?device_id=tv-123"
        val matrix = QrCodeGenerator.generateMatrix(url, 768)
        val pixels = IntArray(matrix.width * matrix.height) { index ->
            if (matrix[index % matrix.width, index / matrix.width]) 0xFF000000.toInt()
            else 0xFFFFFFFF.toInt()
        }
        val source = com.google.zxing.RGBLuminanceSource(matrix.width, matrix.height, pixels)
        val bitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
        assertEquals(url, com.google.zxing.qrcode.QRCodeReader().decode(bitmap).text)
    }

    @Test(expected = IllegalArgumentException::class)
    fun generateMatrix_rejectsBlankContent() {
        QrCodeGenerator.generateMatrix("   ", 256)
    }
}
