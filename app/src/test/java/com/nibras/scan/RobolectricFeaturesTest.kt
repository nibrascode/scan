package com.nibras.scan

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.nibras.scan.tools.QrHistory
import com.nibras.scan.tools.QrHistoryActivity
import com.nibras.scan.tools.SignActivity
import com.nibras.scan.util.PdfTools
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RobolectricFeaturesTest {

    private val ctx get() = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test fun qrHistoryDedupesCapsAndRoundTrips() {
        val e1 = QrHistory.Entry("https://nibrascode.com", "QR", 1)
        val e2 = QrHistory.Entry("4006381333931", "EAN-13", 2)
        var list = QrHistory.merge(emptyList(), e1)
        list = QrHistory.merge(list, e2)
        list = QrHistory.merge(list, e1.copy(time = 3))
        assertEquals(listOf("https://nibrascode.com", "4006381333931"), list.map { it.value })
        assertEquals(list, QrHistory.parse(QrHistory.serialize(list)))
        var big = emptyList<QrHistory.Entry>()
        repeat(250) { big = QrHistory.merge(big, QrHistory.Entry("v$it", "QR", it.toLong())) }
        assertEquals(QrHistory.MAX, big.size)
        assertTrue(QrHistory.isUrl("www.example.com"))
        assertFalse(QrHistory.isUrl("hello world"))
        QrHistory.add(ctx, e1); assertEquals(1, QrHistory.load(ctx).size); QrHistory.clear(ctx)
        assertTrue(QrHistory.load(ctx).isEmpty())
    }

    private fun page(color: Int) = Bitmap.createBitmap(600, 848, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    @Test fun jpegPdfWatermarkSignatureAndPassword() {
        PDFBoxResourceLoader.init(ctx)
        val dir = File(ctx.cacheDir, "t").apply { mkdirs() }
        val out = File(dir, "a.pdf")
        PdfTools.writeJpegPdf(3, out, 80) { i -> page(if (i == 0) Color.WHITE else Color.LTGRAY) }
        assertTrue(out.length() > 0)
        PDDocument.load(out).use { assertEquals(3, it.numberOfPages); assertEquals(842f, it.getPage(0).mediaBox.height, 0.5f) }

        val bmp = page(Color.WHITE)
        PdfTools.drawWatermark(android.graphics.Canvas(bmp), bmp.width, bmp.height, "CONFIDENTIAL")
        val sig = Bitmap.createBitmap(100, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        PdfTools.drawSignature(android.graphics.Canvas(bmp), bmp.width, bmp.height, sig, RectF(0.5f, 0.8f, 0.9f, 0.9f))
        assertEquals(Color.BLUE, bmp.getPixel((0.7f * bmp.width).toInt(), (0.85f * bmp.height).toInt()))

        val locked = File(dir, "locked.pdf")
        PdfTools.protect(out, locked, "s3cret")
        PDDocument.load(locked, "s3cret").use { assertTrue(it.isEncrypted); assertEquals(3, it.numberOfPages) }
        val failed = runCatching { PDDocument.load(locked, "wrong").close() }.isFailure
        assertTrue("wrong password must fail", failed)
    }

    @Test fun mainActivityShowsSixToolCards() {
        val a = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val grid = a.findViewById<LinearLayout>(R.id.toolsGrid)
        val cards = (0 until grid.childCount).sumOf { (grid.getChildAt(it) as LinearLayout).childCount }
        assertEquals(6, cards)
    }

    @Test fun toolScreensOpen() {
        val pdf = File(ctx.cacheDir, "s.pdf")
        PDFBoxResourceLoader.init(ctx)
        PdfTools.writeJpegPdf(1, pdf, 80) { page(Color.WHITE) }
        val sign = Robolectric.buildActivity(SignActivity::class.java,
            Intent(ctx, SignActivity::class.java).putExtra(SignActivity.EXTRA_PDF, pdf.absolutePath)).setup().get()
        assertFalse(sign.isFinishing)
        QrHistory.add(ctx, QrHistory.Entry("hello", "QR", 1))
        val hist = Robolectric.buildActivity(QrHistoryActivity::class.java).setup().get()
        assertFalse(hist.isFinishing)
    }
}
