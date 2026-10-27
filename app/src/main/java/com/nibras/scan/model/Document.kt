package com.nibras.scan.model

import android.graphics.Bitmap
import java.io.File

data class Document(
    val name: String,
    val file: File,
    val pageCount: Int,
    val createdAt: Long,
    val thumb: Bitmap? = null
)
