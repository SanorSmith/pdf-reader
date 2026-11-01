package com.sanorsmith.pdfreader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject

data class RecentFile(val uri: Uri, val name: String, val lastPage: Int)

/** Remembers recently opened PDFs and the last page read in each. */
class RecentFiles(context: Context) {
    private val prefs = context.getSharedPreferences("recent", Context.MODE_PRIVATE)

    fun load(): List<RecentFile> {
        val arr = runCatching { JSONArray(prefs.getString("items", "[]")) }.getOrDefault(JSONArray())
        return (0 until arr.length()).map { arr.getJSONObject(it) }.map {
            RecentFile(Uri.parse(it.getString("uri")), it.getString("name"), it.optInt("page", 0))
        }
    }

    fun add(uri: Uri, name: String, page: Int = 0) {
        val list = load().filterNot { it.uri == uri }
        save(listOf(RecentFile(uri, name, page)) + list.take(19))
    }

    fun savePage(uri: Uri, page: Int) {
        save(load().map { if (it.uri == uri) it.copy(lastPage = page) else it })
    }

    fun lastPage(uri: Uri): Int = load().firstOrNull { it.uri == uri }?.lastPage ?: 0

    fun remove(uri: Uri) = save(load().filterNot { it.uri == uri })

    private fun save(items: List<RecentFile>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("uri", it.uri.toString()).put("name", it.name).put("page", it.lastPage))
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    companion object {
        fun displayName(context: Context, uri: Uri): String =
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && i >= 0) c.getString(i) else null
                }
            }.getOrNull() ?: uri.lastPathSegment ?: "Document.pdf"

        fun keepAccess(context: Context, uri: Uri) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}
