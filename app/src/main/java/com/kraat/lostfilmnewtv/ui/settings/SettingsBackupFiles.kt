package com.kraat.lostfilmnewtv.ui.settings

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class SettingsBackupFiles(private val resolver: ContentResolver) {
    fun write(uri: Uri, content: String) {
        val output = resolver.openOutputStream(uri, "wt") ?: throw IOException("Backup file is unavailable")
        output.use { it.write(content.toByteArray(Charsets.UTF_8)) }
    }

    fun saveInDownloads(content: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) throw IOException("Downloads storage is unavailable")
        val name = newFileName()
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, JSON_TYPE)
            put(MediaStore.Downloads.RELATIVE_PATH, DIRECTORY)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Cannot create backup")
        try {
            write(uri, content)
            val ready = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            if (resolver.update(uri, ready, null, null) != 1) throw IOException("Cannot save backup")
        } catch (error: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
        val actualName = resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: name
        return "Загрузки/LostFilm.TV/$actualName"
    }

    fun read(uri: Uri): String {
        require(uri.scheme == ContentResolver.SCHEME_CONTENT || uri.scheme == ContentResolver.SCHEME_FILE)
        val input = resolver.openInputStream(uri) ?: throw IOException("Backup file is unavailable")
        return input.use {
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(result.size() + count <= MAX_BYTES)
                result.write(buffer, 0, count)
            }
            result.toByteArray().toString(Charsets.UTF_8)
        }
    }

    fun readLatest(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val id = resolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.RELATIVE_PATH} = ? AND ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
            arrayOf(DIRECTORY, "LostFilm.TV-settings-%.json"),
            "${MediaStore.Downloads.DATE_ADDED} DESC, ${MediaStore.Downloads._ID} DESC",
        )?.use { if (it.moveToFirst()) it.getLong(0) else null } ?: return null
        return read(ContentUris.withAppendedId(collection, id))
    }

    companion object {
        const val JSON_TYPE = "application/json"
        private const val MAX_BYTES = 32 * 1024
        private val DIRECTORY = "${Environment.DIRECTORY_DOWNLOADS}/LostFilm.TV/"
        private val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

        fun newFileName(): String = "LostFilm.TV-settings-${LocalDateTime.now().format(timestamp)}.json"
    }
}

internal fun Context.settingsBackupPicker(action: String, mimeType: String): Intent? {
    val intent = Intent(action).addCategory(Intent.CATEGORY_OPENABLE).setType(mimeType)
    val picker = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        .firstOrNull { !it.activityInfo.packageName.endsWith(".frameworkpackagestubs") }
        ?: return null
    return intent.setPackage(picker.activityInfo.packageName)
}
