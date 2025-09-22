package com.example.teost.data.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.teost.data.model.Config
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

object ConfigIO {
    private val gson = Gson()

    fun importConfig(context: Context, uri: Uri): Config? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val reader = BufferedReader(InputStreamReader(input))
                val content = reader.readText()
                gson.fromJson(content, Config::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun exportConfig(
        context: Context,
        config: Config,
        filename: String = "config.json",
        authority: String
    ): Pair<Uri, Intent>? {
        return try {
            val json = gson.toJson(config)
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = context.contentResolver.insert(collection, values) ?: return null
                context.contentResolver.openOutputStream(itemUri)?.use { out ->
                    out.write(json.toByteArray())
                    out.flush()
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(itemUri, values, null, null)
                itemUri
            } else {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloads.exists()) downloads.mkdirs()
                val outFile = File(downloads, filename)
                FileOutputStream(outFile).use { it.write(json.toByteArray()) }
                FileProvider.getUriForFile(context, authority, outFile)
            }
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Pair(uri, share)
        } catch (e: Exception) {
            null
        }
    }
}


