package com.nibras.scan.tools

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local-only history of scanned QR / barcodes (newest first, max 200, de-duplicated). */
object QrHistory {
    data class Entry(val value: String, val format: String, val time: Long)

    private const val PREFS = "nibras_qr"
    private const val KEY = "history"
    const val MAX = 200

    fun load(c: Context): List<Entry> = parse(c.getSharedPreferences(PREFS, 0).getString(KEY, "[]")!!)

    fun add(c: Context, e: Entry) = save(c, merge(load(c), e))

    fun remove(c: Context, e: Entry) = save(c, load(c).filterNot { it == e })

    fun clear(c: Context) = save(c, emptyList())

    private fun save(c: Context, list: List<Entry>) {
        c.getSharedPreferences(PREFS, 0).edit().putString(KEY, serialize(list)).apply()
    }

    fun merge(list: List<Entry>, e: Entry): List<Entry> =
        (listOf(e) + list.filterNot { it.value == e.value }).take(MAX)

    fun serialize(list: List<Entry>): String = JSONArray().apply {
        list.forEach { put(JSONObject().put("v", it.value).put("f", it.format).put("t", it.time)) }
    }.toString()

    fun parse(json: String): List<Entry> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getJSONObject(it).let { o -> Entry(o.getString("v"), o.optString("f"), o.optLong("t")) } }
    } catch (_: Exception) { emptyList() }

    fun isUrl(v: String) = Regex("^(https?://|www\\.)\\S+$", RegexOption.IGNORE_CASE).matches(v.trim())
}
