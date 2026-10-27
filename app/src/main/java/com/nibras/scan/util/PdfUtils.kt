package com.nibras.scan.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfUtils {

    /**
     * Writes [pageCount] pages into [outputFile]. Bitmaps are produced one at a time by
     * [bitmapProvider] and recycled right after drawing. Each PDF page keeps the bitmap's
     * own aspect ratio. The file is only replaced when the whole document is written.
     */
    fun createPdf(pageCount: Int, outputFile: File, bitmapProvider: (Int) -> Bitmap) {
        val document = PdfDocument()
        val tmp = File(outputFile.parentFile, outputFile.name + ".tmp")
        try {
            for (index in 0 until pageCount) {
                val bitmap = bitmapProvider(index)
                try {
                    val info = PdfDocument.PageInfo.Builder(
                        bitmap.width.coerceAtLeast(1),
                        bitmap.height.coerceAtLeast(1),
                        index + 1
                    ).create()
                    val page = document.startPage(info)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    document.finishPage(page)
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
            FileOutputStream(tmp).use { out -> document.writeTo(out) }
            if (outputFile.exists() && !outputFile.delete()) {
                throw IllegalStateException("replace")
            }
            if (!tmp.renameTo(outputFile)) {
                tmp.copyTo(outputFile, overwrite = true)
                tmp.delete()
            }
        } catch (t: Throwable) {
            tmp.delete()
            throw t
        } finally {
            document.close()
        }
    }

    /** Page count plus a small first-page thumbnail. Cached beside the PDF so the list stays fast. */
    fun inspect(file: File, thumbSize: Int = 160): Pair<Int, Bitmap?> {
        val stamp = "${file.length()}:${file.lastModified()}"
        val meta = sidecar(file, ".nibrasmeta")
        val thumbFile = sidecar(file, ".thumb.jpg")
        if (meta.exists() && thumbFile.exists()) {
            val lines = meta.readLines()
            if (lines.size >= 2 && lines[0] == stamp) {
                val count = lines[1].toIntOrNull() ?: 0
                val thumb = BitmapFactory.decodeFile(thumbFile.absolutePath)
                return count to thumb
            }
        }
        val fresh = renderInspect(file, thumbSize)
        try {
            meta.writeText(stamp + "\n" + fresh.first)
            fresh.second?.let { bmp ->
                FileOutputStream(thumbFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
            }
        } catch (_: Exception) {
        }
        return fresh
    }

    fun deleteSidecars(file: File) {
        sidecar(file, ".nibrasmeta").delete()
        sidecar(file, ".thumb.jpg").delete()
    }

    fun moveSidecars(from: File, to: File) {
        sidecar(from, ".nibrasmeta").let { if (it.exists()) it.renameTo(sidecar(to, ".nibrasmeta")) }
        sidecar(from, ".thumb.jpg").let { if (it.exists()) it.renameTo(sidecar(to, ".thumb.jpg")) }
    }

    private fun sidecar(file: File, suffix: String): File {
        return File(file.parentFile, file.nameWithoutExtension + suffix)
    }

    private fun renderInspect(file: File, thumbSize: Int): Pair<Int, Bitmap?> {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount
                    if (count == 0) return 0 to null
                    renderer.openPage(0).use { page ->
                        val w = thumbSize
                        val h = (thumbSize.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        count to bmp
                    }
                }
            }
        } catch (_: Exception) {
            0 to null
        }
    }
}
