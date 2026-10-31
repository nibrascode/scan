package com.nibras.scan.util

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Builds the searchable text of a PDF on the device (ML Kit Latin, plus Tesseract
 * Arabic when that OCR language is chosen) and stores it beside the PDF.
 */
object OcrIndexer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()

    fun indexAsync(context: Context, pdf: File, onDone: (() -> Unit)? = null) {
        val app = context.applicationContext
        scope.launch {
            lock.withLock { runCatching { index(app, pdf) } }
            onDone?.let { launch(Dispatchers.Main) { it() } }
        }
    }

    /** Blocking; call off the main thread. Returns the recognised text. */
    fun index(context: Context, pdf: File): String {
        val count = PdfTools.pageCount(pdf)
        val sb = StringBuilder()
        val arabic = SettingsManager.getOcrLanguage(context) == SettingsManager.OcrLanguage.ARABIC
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            for (i in 0 until count) {
                val bmp = PdfTools.renderPage(pdf, i, 2000)
                try {
                    val text = if (arabic) tesseract(context, bmp)
                    else Tasks.await(recognizer.process(InputImage.fromBitmap(bmp, 0))).text
                    if (count > 1) sb.append("— ").append(i + 1).append(" —\n")
                    sb.append(text.trim()).append("\n\n")
                } finally {
                    bmp.recycle()
                }
            }
        } finally {
            recognizer.close()
        }
        val result = sb.toString().trim()
        if (pdf.exists()) Library.ocrFile(pdf).writeText(result)
        return result
    }

    private fun tesseract(context: Context, bmp: android.graphics.Bitmap): String {
        if (!TesseractHelper.ensureLanguageAvailable(context, "ara")) return ""
        val tess = TessBaseAPI()
        return try {
            val langs = if (TesseractHelper.ensureLanguageAvailable(context, "eng")) "ara+eng" else "ara"
            if (!tess.init(TesseractHelper.tessDataParentDir(context).absolutePath, langs)) return ""
            tess.setImage(bmp)
            tess.getUTF8Text() ?: ""
        } finally {
            runCatching { tess.recycle() }
        }
    }
}
