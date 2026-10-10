package com.pcbrowser.chrome.bookmarks

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class BookmarkManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_BOOKMARKS, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_BOOKMARKS = "deskweb_bookmarks"
        private const val KEY_PREFIX = "bookmarks_"
    }

    fun getBookmarks(profileId: String): MutableList<BookmarkModel> {
        val jsonStr = prefs.getString(KEY_PREFIX + profileId, null)
        if (jsonStr == null) {
            // Seed defaults
            val defaults = mutableListOf(
                BookmarkModel("bm_1", "Google", "https://www.google.com"),
                BookmarkModel("bm_2", "YouTube", "https://www.youtube.com"),
                BookmarkModel("bm_3", "GitHub", "https://github.com"),
                BookmarkModel("bm_4", "Wikipedia", "https://www.wikipedia.org"),
                BookmarkModel("bm_5", "Reddit", "https://www.reddit.com")
            )
            saveBookmarks(profileId, defaults)
            return defaults
        }

        val list = mutableListOf<BookmarkModel>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BookmarkModel(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        url = obj.getString("url")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveBookmarks(profileId: String, bookmarks: List<BookmarkModel>) {
        val array = JSONArray()
        for (b in bookmarks) {
            val obj = JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("url", b.url)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PREFIX + profileId, array.toString()).apply()
    }

    fun addBookmark(profileId: String, title: String, url: String): BookmarkModel {
        val id = "bm_${System.currentTimeMillis()}"
        val cleanTitle = if (title.isBlank()) url else title
        val bm = BookmarkModel(id, cleanTitle, url)
        val list = getBookmarks(profileId)
        list.add(bm)
        saveBookmarks(profileId, list)
        return bm
    }

    fun removeBookmark(profileId: String, id: String) {
        val list = getBookmarks(profileId)
        list.removeAll { it.id == id }
        saveBookmarks(profileId, list)
    }

    fun updateBookmark(profileId: String, id: String, newTitle: String, newUrl: String): Boolean {
        val list = getBookmarks(profileId)
        val item = list.find { it.id == id } ?: return false
        item.title = newTitle.ifBlank { newUrl }
        item.url = newUrl
        saveBookmarks(profileId, list)
        return true
    }

    fun isBookmarked(profileId: String, url: String): Boolean {
        if (url.isBlank() || url == "about:blank") return false
        val norm = normalizeUrl(url)
        return getBookmarks(profileId).any { normalizeUrl(it.url) == norm }
    }

    fun getBookmarkByUrl(profileId: String, url: String): BookmarkModel? {
        if (url.isBlank() || url == "about:blank") return null
        val norm = normalizeUrl(url)
        return getBookmarks(profileId).find { normalizeUrl(it.url) == norm }
    }

    private fun normalizeUrl(url: String): String {
        return url.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removeSuffix("/webhp")
            .removeSuffix("/")
            .lowercase()
    }
}
