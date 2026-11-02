package com.eymen.uydukontrol

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import java.io.File

/** Read-only: gives EYMEN Başlatıcı the channel list and the last channel watched in EYMEN TV. */
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

    override fun getType(uri: Uri): String? = "application/json"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
