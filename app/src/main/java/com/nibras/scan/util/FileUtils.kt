package com.nibras.scan.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FileUtils {

    private val illegalName = Regex("""[\\/:*?"<>|\p{Cntrl}]""")

    fun documentsDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "documents")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun pagesDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "pages")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun newPageFile(context: Context): File {
        return File(pagesDir(context), "page_${UUID.randomUUID()}.jpg")
    }

    fun saveBitmap(bitmap: Bitmap, target: File, quality: Int = 92) {
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(50, 100), out)
        }
    }

    /** Decodes so the longest side is roughly <= [maxDim]. Null if the file is unreadable. */
    fun decodeSampled(path: String, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / (sample * 2) >= maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(path, opts)
    }

    fun sanitizeDocumentName(raw: String): String {
        return raw.trim()
            .removeSuffix(".pdf")
            .removeSuffix(".PDF")
            .replace(illegalName, " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
            .take(80)
    }

    fun defaultDocumentName(): String {
        val fmt = SimpleDateFormat("yyyy_MM_dd_HHmm", Locale.getDefault())
        return "Senad_${fmt.format(Date())}"
    }

    fun listDocuments(context: Context): List<File> {
        val dir = documentsDir(context)
        return dir.listFiles { f -> f.isFile && f.extension.equals("pdf", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun clearPagesDir(context: Context) {
        pagesDir(context).listFiles()?.forEach { it.delete() }
    }

    /** Copies a gallery Uri into cache so the rest of the pipeline can use a real file. */
    fun copyUriToCache(context: Context, uri: Uri): File? {
        return try {
            val dest = File(context.cacheDir, "import_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            } ?: return null
            if (!dest.exists() || dest.length() == 0L) {
                dest.delete()
                null
            } else dest
        } catch (_: Exception) {
            null
        }
    }

    fun shareUriFor(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun buildShareIntent(context: Context, file: File): Intent {
        val uri = shareUriFor(context, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri("pdf", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun buildViewIntent(context: Context, file: File): Intent {
        val uri = shareUriFor(context, file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            clipData = android.content.ClipData.newRawUri("pdf", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
