package com.nibras.scan.util

import android.content.Context
import com.nibras.scan.model.FilterType

/** User preferences stored locally in SharedPreferences (never leaves the device). */
object SettingsManager {

    private const val PREFS = "nibras_settings"
    private const val KEY_FILTER = "default_filter"
    private const val KEY_QUALITY = "pdf_quality"
    private const val KEY_OCR_LANG = "ocr_language"

    enum class PdfQuality(val maxDimensionPx: Int, val jpegQuality: Int) {
        LOW(1240, 72),
        MEDIUM(1754, 86),
        HIGH(3000, 94)
    }

    enum class OcrLanguage { LATIN, ARABIC }

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getDefaultFilter(c: Context): FilterType = try {
        FilterType.valueOf(prefs(c).getString(KEY_FILTER, FilterType.AUTO.name)!!)
    } catch (e: Exception) { FilterType.AUTO }

    fun setDefaultFilter(c: Context, f: FilterType) {
        prefs(c).edit().putString(KEY_FILTER, f.name).apply()
    }

    fun getPdfQuality(c: Context): PdfQuality = try {
        PdfQuality.valueOf(prefs(c).getString(KEY_QUALITY, PdfQuality.MEDIUM.name)!!)
    } catch (e: Exception) { PdfQuality.MEDIUM }

    fun setPdfQuality(c: Context, q: PdfQuality) {
        prefs(c).edit().putString(KEY_QUALITY, q.name).apply()
    }

    fun getOcrLanguage(c: Context): OcrLanguage = try {
        OcrLanguage.valueOf(prefs(c).getString(KEY_OCR_LANG, OcrLanguage.LATIN.name)!!)
    } catch (e: Exception) { OcrLanguage.LATIN }

    fun setOcrLanguage(c: Context, l: OcrLanguage) {
        prefs(c).edit().putString(KEY_OCR_LANG, l.name).apply()
    }
}
