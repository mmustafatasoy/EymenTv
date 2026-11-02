package com.eymen.uydukontrol

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/**
 * Read-only: gives EYMEN OS and EYMEN Başlatıcı the channel list, the last channel watched in
 * EYMEN TV and the logos the user added (content://com.eymen.uydukontrol.state/logo/<channel key>).
 */
class StateProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val ctx = context ?: return null
        val json = try {
            val f = File(ctx.filesDir, "launcher_state.json")
            if (f.exists()) f.readText() else "{}"
        } catch (e: Exception) {
            "{}"
        }
        val cursor = MatrixCursor(arrayOf("json"))
        cursor.addRow(arrayOf<Any>(json))
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val ctx = context ?: throw FileNotFoundException("Hazır değil")
        val segments = uri.pathSegments
        if (mode != "r" || segments.size != 2 || segments[0] != "logo") throw FileNotFoundException(uri.toString())
        val f = Logos.customFile(ctx, segments[1])
        if (!f.exists()) throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String? = "application/json"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
