package com.dhyper.fncompanion.ui.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object FileSharingUtils {

    fun shareBitmap(context: Context, bitmap: Bitmap, filename: String) {
        val uri = saveBitmapToCache(context, bitmap, filename) ?: return
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Locker Image"))
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filename: String): Boolean {
        val name = "${filename}_${System.currentTimeMillis()}.png"
        val out: OutputStream?
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FortniteCompanion")
            }
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false
            out = resolver.openOutputStream(imageUri)
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
            val image = File(imagesDir, name)
            out = FileOutputStream(image)
        }

        return try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out!!)
            out.flush()
            out.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap, filename: String): Uri? {
        return try {
            val cachePath = File(context.externalCacheDir, "locker_exports")
            cachePath.mkdirs()
            val file = File(cachePath, "$filename.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }
}
