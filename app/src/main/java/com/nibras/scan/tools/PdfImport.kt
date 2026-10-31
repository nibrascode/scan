package com.nibras.scan.tools

import android.content.Context
import android.net.Uri
import com.nibras.scan.model.Page
import com.nibras.scan.repo.ScanSession
import com.nibras.scan.util.FileUtils
import com.nibras.scan.util.PdfTools
import com.nibras.scan.util.SettingsManager
import java.io.File
import java.util.UUID

/** Turns PDFs into editable pages of the scan session (edit pages, merge, reorder…). */
object PdfImport {

    /** Copies a picked PDF into cache. */
    fun copyPdf(context: Context, uri: Uri): File? = try {
        val dest = File(context.cacheDir, "pdf_${UUID.randomUUID()}.pdf")
        context.contentResolver.openInputStream(uri)?.use { i -> dest.outputStream().use { i.copyTo(it) } }
        dest.takeIf { it.length() > 0 }
    } catch (_: Exception) { null }

    /** Appends all pages of [pdfs] to the session. Returns pages added (0 = unreadable). */
    fun loadIntoSession(context: Context, pdfs: List<File>): Int {
        val q = SettingsManager.getPdfQuality(context)
        var added = 0
        for (pdf in pdfs) {
            val count = PdfTools.pageCount(pdf)
            for (i in 0 until count) {
                val bmp = PdfTools.renderPage(pdf, i, q.maxDimensionPx)
                val f = FileUtils.newPageFile(context)
                FileUtils.saveBitmap(bmp, f, 95)
                bmp.recycle()
                ScanSession.addPage(Page(originalFile = f))
                added++
            }
        }
        return added
    }
}
