package com.example.dosezy.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import kotlin.math.max

object ImageUtils {
    private const val TAG = "ImageUtils"

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

    /**
     * Calculates the power-of-2 inSampleSize for downsampling large images.
     * Guard: inSampleSize must be a power of 2 for optimal decoding efficiency and memory reduction.
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Saves a downsampled and compressed version of an image from a content URI to a destination file.
     * Guard: Downsamples camera/gallery bitmaps and corrects EXIF rotation to prevent OutOfMemory crashes on high-res photos.
     */
    fun saveDownsampledImage(
        context: Context,
        uri: Uri,
        destFile: File,
        maxDimension: Int = 1024,
        quality: Int = 85
    ): String {
        try {
            destFile.parentFile?.mkdirs()

            // Step 1: Decode image bounds without allocating memory
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val outWidth = boundsOptions.outWidth
            val outHeight = boundsOptions.outHeight

            if (outWidth <= 0 || outHeight <= 0) {
                // Guard: Fallback to raw copy if dimensions cannot be determined to prevent data loss
                copyRawStream(context, uri, destFile)
                return destFile.absolutePath
            }

            // Step 2: Compute inSampleSize
            val sampleSize = calculateInSampleSize(boundsOptions, maxDimension, maxDimension)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            // Step 3: Decode downsampled bitmap
            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: run {
                copyRawStream(context, uri, destFile)
                return destFile.absolutePath
            }

            // Step 4: Correct EXIF orientation for camera captures
            val orientation = getExifOrientation(context, uri)
            val orientedBitmap = applyExifOrientation(decodedBitmap, orientation)

            // Step 5: Final scale down if dimensions still exceed maxDimension
            val finalBitmap = if (max(orientedBitmap.width, orientedBitmap.height) > maxDimension) {
                val scaleRatio = maxDimension.toFloat() / max(orientedBitmap.width, orientedBitmap.height)
                val targetW = (orientedBitmap.width * scaleRatio).toInt().coerceAtLeast(1)
                val targetH = (orientedBitmap.height * scaleRatio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(orientedBitmap, targetW, targetH, true)
                if (scaled != orientedBitmap && orientedBitmap != decodedBitmap) {
                    orientedBitmap.recycle()
                }
                scaled
            } else {
                orientedBitmap
            }

            // Step 6: Compress to destination JPEG
            destFile.outputStream().use { output ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            }

            // Clean up temporary bitmaps
            if (decodedBitmap != finalBitmap && !decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }
            if (orientedBitmap != finalBitmap && !orientedBitmap.isRecycled) {
                orientedBitmap.recycle()
            }
            if (!finalBitmap.isRecycled) {
                finalBitmap.recycle()
            }

            Log.d(TAG, "Downsampled image saved to: ${destFile.absolutePath} (${destFile.length()} bytes)")
            return destFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Downsampling failed, falling back to raw stream copy: ${e.message}")
            copyRawStream(context, uri, destFile)
            return destFile.absolutePath
        }
    }

    private fun copyRawStream(context: Context, uri: Uri, destFile: File) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream for $uri")
        inputStream.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            // Guard: API 24+ ExifInterface(InputStream) reads orientation tags from content URIs
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Throwable) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (_: Throwable) {
            bitmap
        }
    }
}
