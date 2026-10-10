package com.pcbrowser.chrome.history

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class HistoryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_HISTORY = "deskweb_history"
        private const val KEY_PREFIX = "history_profile_"
        private const val MAX_ENTRIES = 1000
    }

    @Synchronized
    fun addEntry(profileId: String, url: String, title: String) {
        if (url.isEmpty() || url == "about:blank" || url.startsWith("data:") || profileId == "profile_guest") {
            return
        }

        val entries = getHistory(profileId).toMutableList()
        // Deduplicate recent identical URLs if added within 5 seconds
        if (entries.isNotEmpty() && entries[0].url == url && System.currentTimeMillis() - entries[0].timestamp < 5000) {
            return
        }

        val displayTitle = if (title.isNotBlank()) title else url
        val newEntry = HistoryModel(
            id = System.currentTimeMillis(),
            url = url,
            title = displayTitle,
            timestamp = System.currentTimeMillis()
        )

        entries.add(0, newEntry)
        if (entries.size > MAX_ENTRIES) {
            entries.removeAt(entries.size - 1)
        }

        saveHistory(profileId, entries)
    }

    @Synchronized
    fun getHistory(profileId: String): List<HistoryModel> {
        val jsonStr = prefs.getString(KEY_PREFIX + profileId, null) ?: return emptyList()
        val list = mutableListOf<HistoryModel>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HistoryModel(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        url = obj.getString("url"),
                        title = obj.optString("title", obj.getString("url")),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    @Synchronized
    fun deleteEntry(profileId: String, id: Long) {
        val list = getHistory(profileId).toMutableList()
        list.removeAll { it.id == id }
        saveHistory(profileId, list)
    }

    @Synchronized
    fun clearHistory(profileId: String) {
        prefs.edit().remove(KEY_PREFIX + profileId).apply()
    }

    private fun saveHistory(profileId: String, list: List<HistoryModel>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("url", item.url)
                put("title", item.title)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PREFIX + profileId, array.toString()).apply()
    }
}
