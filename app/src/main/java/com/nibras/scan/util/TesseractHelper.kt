package com.nibras.scan.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * Tesseract needs its `.traineddata` language files sitting on the filesystem (it cannot
 * read them straight out of the APK's assets). This copies whatever language files the
 * developer has bundled under `assets/tessdata/` into internal storage the first time
 * they're needed.
 *
 * `assets/tessdata/` already bundles `ara.traineddata` and `eng.traineddata`.
 * They are copied into internal storage the first time Arabic OCR runs (Tesseract
 * cannot read them straight from the APK). Latin OCR uses ML Kit and does not need these files.
 */
object TesseractHelper {

    fun tessDataParentDir(context: Context): File {
        val dir = File(context.filesDir, "tesseract")
        if (!dir.exists()) dir.mkdirs()
        val tessdata = File(dir, "tessdata")
        if (!tessdata.exists()) tessdata.mkdirs()
        return dir
    }

    /** Returns true if the requested language file is available (either already copied,
     * or successfully copied from assets just now). */
    fun ensureLanguageAvailable(context: Context, languageCode: String): Boolean {
        val parentDir = tessDataParentDir(context)
        val targetFile = File(parentDir, "tessdata/$languageCode.traineddata")
        if (targetFile.exists()) return true

        return try {
            context.assets.open("tessdata/$languageCode.traineddata").use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            // Language file was not bundled in assets/tessdata/ - see class doc above.
            false
        }
    }
}
