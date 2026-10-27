package com.nibras.scan.util

import android.graphics.Bitmap
import android.util.LruCache
import com.nibras.scan.model.FilterType
import com.nibras.scan.model.Page
import java.io.File

/** Keeps a few filtered previews so paging and filter taps don't decode again. */
object PreviewCache {

    private val pages = LruCache<String, Bitmap>(4)
    private val thumbs = LruCache<String, Bitmap>(24)

    fun render(path: String, rotation: Int, filter: FilterType, maxDim: Int): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        val key = "$path|$rotation|${filter.name}|$maxDim|${file.length()}|${file.lastModified()}"
        val bucket = if (maxDim <= 400) thumbs else pages
        bucket.get(key)?.let { if (!it.isRecycled) return it }
        val created = try {
            val src = FileUtils.decodeSampled(path, maxDim) ?: return null
            FilterUtils.render(
                src,
                Page(originalFile = file, rotationDegrees = rotation, filter = filter)
            )
        } catch (_: Throwable) {
            null
        } ?: return null
        val raced = bucket.get(key)
        if (raced != null && !raced.isRecycled) {
            if (raced !== created && !created.isRecycled) created.recycle()
            return raced
        }
        bucket.put(key, created)
        return created
    }
}
