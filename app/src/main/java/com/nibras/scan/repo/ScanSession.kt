package com.nibras.scan.repo

import android.content.Context
import com.nibras.scan.model.FilterType
import com.nibras.scan.model.Page
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Pages of the document currently being scanned. Backed by a small JSON file so a
 * process kill (common while the camera or crop screen is open) does not throw the
 * scan away. Image bytes stay in the pages directory; this only stores paths.
 */
object ScanSession {

    private const val PREFS = "nibras_session"
    private const val KEY_PAGES = "pages"
    private const val KEY_PENDING = "pending"

    private val pages = mutableListOf<Page>()
    private val pendingRaw = ArrayDeque<String>()
    private var appContext: Context? = null

    fun restore(context: Context) {
        appContext = context.applicationContext
        if (pages.isNotEmpty()) return
        val prefs = appContext!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PAGES, null)
        if (!raw.isNullOrEmpty()) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val file = File(o.getString("file"))
                    if (!file.exists()) continue
                    val filter = try {
                        FilterType.valueOf(o.getString("filter"))
                    } catch (_: Exception) {
                        FilterType.AUTO
                    }
                    pages.add(
                        Page(
                            id = o.getString("id"),
                            originalFile = file,
                            rotationDegrees = o.optInt("rot", 0),
                            filter = filter
                        )
                    )
                }
            } catch (_: Exception) {
                pages.clear()
            }
        }
        val pend = prefs.getString(KEY_PENDING, null).orEmpty()
        if (pend.isNotEmpty()) {
            pend.split('|').filter { it.isNotEmpty() && File(it).exists() }.forEach { pendingRaw.add(it) }
        }
    }

    fun getPages(): List<Page> = pages

    fun pagesMutable(): MutableList<Page> = pages

    fun addPage(page: Page) {
        pages.add(page)
        persist()
    }

    fun removePage(pageId: String) {
        pages.removeAll { it.id == pageId }
        persist()
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in pages.indices || toIndex !in pages.indices) return
        val page = pages.removeAt(fromIndex)
        pages.add(toIndex, page)
        persist()
    }

    fun getPage(pageId: String): Page? = pages.find { it.id == pageId }

    fun isEmpty(): Boolean = pages.isEmpty()

    fun size(): Int = pages.size

    /** Call after in-place edits (rotation, filter, swapped file) that don't go through add/remove. */
    fun touch() = persist()

    fun enqueueRaw(paths: List<String>) {
        pendingRaw.addAll(paths)
        persist()
    }

    fun pollRaw(): String? {
        val next = pendingRaw.removeFirstOrNull()
        persist()
        return next
    }

    fun clear() {
        pages.clear()
        pendingRaw.forEach { path ->
            try { File(path).delete() } catch (_: Exception) {}
        }
        pendingRaw.clear()
        persist()
    }

    private fun persist() {
        val context = appContext ?: return
        val arr = JSONArray()
        for (page in pages) {
            if (!page.originalFile.exists()) continue
            arr.put(JSONObject().apply {
                put("id", page.id)
                put("file", page.originalFile.absolutePath)
                put("rot", page.rotationDegrees)
                put("filter", page.filter.name)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PAGES, arr.toString())
            .putString(KEY_PENDING, pendingRaw.joinToString("|"))
            .apply()
    }
}
