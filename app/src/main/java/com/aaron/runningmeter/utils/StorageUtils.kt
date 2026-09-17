package com.aaron.runningmeter.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.OutputStream

object StorageUtils {

    fun saveRouteImageToGallery(context: Context, image: Bitmap): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "route")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            try {
                val outstream: OutputStream? = context.contentResolver.openOutputStream(it)
                outstream?.use { stream ->
                    image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
            } catch (e: Exception) {
                System.err.println(e.toString())
                return null
            }
        }
        return uri
    }
}
