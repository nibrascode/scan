package com.nibras.scan.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import java.io.File
import java.io.FileOutputStream

/** On-device PDF tools: render, rebuild, watermark, sign, protect, export. Nothing leaves the device. */
object PdfTools {

    /** A4 long side in PDF points. */
    private const val LONG_SIDE_PT = 842f

    fun pageCount(file: File): Int = try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { it.pageCount }
        }
    } catch (_: Exception) { 0 }

    /** Renders one page so its longest side is [maxDim] px, white background. */
    fun renderPage(file: File, index: Int, maxDim: Int): Bitmap {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { r ->
                r.openPage(index).use { page ->
                    val scale = maxDim.toFloat() / maxOf(page.width, page.height)
                    val w = (page.width * scale).toInt().coerceAtLeast(1)
                    val h = (page.height * scale).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    return bmp
                }
            }
        }
    }

    /** Writes JPEG-compressed pages; page size scaled so the long side is A4. */
    fun writeJpegPdf(pageCount: Int, outputFile: File, jpegQuality: Int, provider: (Int) -> Bitmap) {
        val tmp = File(outputFile.parentFile, outputFile.name + ".tmp")
        PDDocument().use { doc ->
            for (i in 0 until pageCount) {
                val bmp = provider(i)
                try {
                    val s = LONG_SIDE_PT / maxOf(bmp.width, bmp.height)
                    val page = PDPage(PDRectangle(bmp.width * s, bmp.height * s))
                    doc.addPage(page)
                    val img = JPEGFactory.createFromImage(doc, bmp, jpegQuality.coerceIn(30, 100) / 100f)
                    PDPageContentStream(doc, page).use { cs ->
                        cs.drawImage(img, 0f, 0f, page.mediaBox.width, page.mediaBox.height)
                    }
                } finally {
                    if (!bmp.isRecycled) bmp.recycle()
                }
            }
            FileOutputStream(tmp).use { doc.save(it) }
        }
        if (outputFile.exists()) outputFile.delete()
        if (!tmp.renameTo(outputFile)) { tmp.copyTo(outputFile, true); tmp.delete() }
    }

    /** Re-renders [input] into [output], letting [decorate] draw on each page bitmap. */
    fun rebuild(
        input: File, output: File, maxDim: Int, jpegQuality: Int,
        decorate: (index: Int, canvas: Canvas, w: Int, h: Int) -> Unit = { _, _, _, _ -> }
    ) {
        val count = pageCount(input)
        require(count > 0) { "empty or unreadable pdf" }
        writeJpegPdf(count, output, jpegQuality) { i ->
            val bmp = renderPage(input, i, maxDim)
            decorate(i, Canvas(bmp), bmp.width, bmp.height)
            bmp
        }
    }

    fun drawWatermark(canvas: Canvas, w: Int, h: Int, text: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 30, 60, 120)
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val diag = Math.hypot(w.toDouble(), h.toDouble()).toFloat()
        paint.textSize = 10f
        val measured = paint.measureText(text).coerceAtLeast(1f)
        paint.textSize = (10f * diag * 0.7f / measured).coerceAtMost(h / 6f)
        canvas.save()
        canvas.rotate(-Math.toDegrees(Math.atan2(h.toDouble(), w.toDouble())).toFloat(), w / 2f, h / 2f)
        canvas.drawText(text, w / 2f, h / 2f - (paint.descent() + paint.ascent()) / 2, paint)
        canvas.restore()
    }

    /** Draws [signature] into a rect given as fractions (0..1) of the page. */
    fun drawSignature(canvas: Canvas, w: Int, h: Int, signature: Bitmap, rel: RectF) {
        val dst = RectF(rel.left * w, rel.top * h, rel.right * w, rel.bottom * h)
        canvas.drawBitmap(signature, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    fun rotate(bmp: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bmp
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    /** AES-128 password protection. Printing allowed; editing needs the same password. */
    fun protect(input: File, output: File, password: String) {
        require(password.isNotEmpty())
        PDDocument.load(input).use { doc ->
            val policy = StandardProtectionPolicy(password, password, AccessPermission().apply {
                setCanModify(false)
            })
            policy.encryptionKeyLength = 128
            doc.protect(policy)
            FileOutputStream(output).use { doc.save(it) }
        }
    }

    /** Exports each page as a JPG into [dir]; returns the files. */
    fun exportJpgs(input: File, dir: File, maxDim: Int, quality: Int): List<File> {
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        val count = pageCount(input)
        return (0 until count).map { i ->
            val bmp = renderPage(input, i, maxDim)
            val f = File(dir, "${input.nameWithoutExtension}_${i + 1}.jpg")
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            bmp.recycle()
            f
        }
    }
}
