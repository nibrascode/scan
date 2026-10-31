package com.nibras.scan.util

import java.io.File
import java.text.Normalizer
import java.util.Locale

/**
 * Document library logic: folders are plain sub-directories of the documents dir,
 * OCR text lives in a "<name>.ocr.txt" sidecar next to each PDF so search can look
 * inside documents. Pure file logic so it is unit-testable.
 */
object Library {

    enum class Sort { DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_DESC }

    const val OCR_SUFFIX = ".ocr.txt"

    fun folders(root: File): List<File> =
        root.listFiles { f -> f.isDirectory && !f.name.startsWith(".") }
            ?.sortedBy { it.name.lowercase(Locale.ROOT) } ?: emptyList()

    fun pdfsIn(dir: File): List<File> =
        dir.listFiles { f -> f.isFile && f.extension.equals("pdf", true) }?.toList() ?: emptyList()

    /** All PDFs in root and every folder. */
    fun allPdfs(root: File): List<File> = pdfsIn(root) + folders(root).flatMap { pdfsIn(it) }

    fun createFolder(root: File, rawName: String): File? {
        val name = FileUtils.sanitizeDocumentName(rawName)
        if (name.isEmpty()) return null
        val dir = File(root, name)
        if (dir.exists()) return if (dir.isDirectory) dir else null
        return if (dir.mkdirs()) dir else null
    }

    fun renameFolder(folder: File, rawName: String): File? {
        val name = FileUtils.sanitizeDocumentName(rawName)
        if (name.isEmpty()) return null
        val target = File(folder.parentFile, name)
        if (target.exists()) return null
        return if (folder.renameTo(target)) target else null
    }

    /** Deletes the folder but keeps its documents by moving them up to root. */
    fun deleteFolderKeepDocs(root: File, folder: File) {
        pdfsIn(folder).forEach { move(it, root) }
        folder.listFiles()?.forEach { it.delete() }
        folder.delete()
    }

    fun uniqueFile(dir: File, baseName: String, ext: String = "pdf"): File {
        var f = File(dir, "$baseName.$ext")
        var i = 2
        while (f.exists()) { f = File(dir, "$baseName ($i).$ext"); i++ }
        return f
    }

    /** Moves a PDF (and its sidecars) into [targetDir]; returns the new file. */
    fun move(pdf: File, targetDir: File): File {
        if (pdf.parentFile?.absolutePath == targetDir.absolutePath) return pdf
        targetDir.mkdirs()
        val dest = uniqueFile(targetDir, pdf.nameWithoutExtension)
        if (!pdf.renameTo(dest)) { pdf.copyTo(dest, true); pdf.delete() }
        sidecars(pdf).zip(sidecars(dest)).forEach { (a, b) -> if (a.exists()) { a.renameTo(b) } }
        return dest
    }

    fun sidecars(pdf: File): List<File> = listOf(".nibrasmeta", ".thumb.jpg", OCR_SUFFIX)
        .map { File(pdf.parentFile, pdf.nameWithoutExtension + it) }

    fun ocrFile(pdf: File) = File(pdf.parentFile, pdf.nameWithoutExtension + OCR_SUFFIX)

    fun ocrText(pdf: File): String = ocrFile(pdf).let { if (it.exists()) it.readText() else "" }

    fun normalize(s: String): String {
        val lower = s.lowercase(Locale.ROOT)
            .replace('ı', 'i').replace('ə', 'e')
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")
    }

    /** True if the query matches the name or (when [text] given) the OCR text. */
    fun matches(name: String, text: String, query: String): Boolean {
        val q = normalize(query.trim())
        if (q.isEmpty()) return true
        return normalize(name).contains(q) || normalize(text).contains(q)
    }

    fun <T> sort(items: List<T>, sort: Sort, name: (T) -> String, date: (T) -> Long, size: (T) -> Long): List<T> =
        when (sort) {
            Sort.DATE_DESC -> items.sortedByDescending(date)
            Sort.DATE_ASC -> items.sortedBy(date)
            Sort.NAME_ASC -> items.sortedBy { name(it).lowercase(Locale.ROOT) }
            Sort.NAME_DESC -> items.sortedByDescending { name(it).lowercase(Locale.ROOT) }
            Sort.SIZE_DESC -> items.sortedByDescending(size)
        }
}
