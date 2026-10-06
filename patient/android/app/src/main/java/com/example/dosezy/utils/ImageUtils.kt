package com.example.dosezy.utils

import android.net.Uri
import java.io.File

object ImageUtils {
    /**
     * Resolves an image path or URI string into a Coil-safe model data object.
     * Guard: Linux/Android File(path) fails on 'file://' prefixed strings; strip prefix or parse Uri to prevent Coil FileNotFoundException.
     */
    fun resolveImageModel(imagePath: String?): Any? {
        if (imagePath.isNullOrEmpty()) return null
        val cleanPath = imagePath.removePrefix("file://")
        val file = File(cleanPath)
        return if (file.exists()) file else Uri.parse(imagePath)
    }
}
