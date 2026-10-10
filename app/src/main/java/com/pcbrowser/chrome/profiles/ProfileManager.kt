package com.pcbrowser.chrome.profiles

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.pcbrowser.chrome.data.SettingsPreferences
import org.json.JSONArray
import org.json.JSONObject

class ProfileManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_PROFILES, Context.MODE_PRIVATE)
    private val settings = SettingsPreferences(context)

    companion object {
        private const val PREFS_PROFILES = "deskweb_profiles"
        private const val KEY_PROFILES_JSON = "key_profiles_json"
        private const val KEY_PREFIX_TABS = "profile_tabs_"

        val PRESET_COLORS = intArrayOf(
            0xFF1A73E8.toInt(), // Google Blue
            0xFF1E8E3E.toInt(), // Green
            0xFFD93025.toInt(), // Red
            0xFFF29900.toInt(), // Yellow/Orange
            0xFF9334E6.toInt(), // Purple
            0xFF007B83.toInt()  // Teal
        )
    }

    init {
        ensureDefaultProfiles()
    }

    private fun ensureDefaultProfiles() {
        val current = getProfiles()
        if (current.isEmpty()) {
            val defaults = listOf(
                ProfileModel("profile_personal", "Personal", PRESET_COLORS[0]),
                ProfileModel("profile_work", "Work", PRESET_COLORS[1])
            )
            saveProfiles(defaults)
            if (settings.activeProfileId.isEmpty()) {
                settings.activeProfileId = "profile_personal"
            }
        }
    }

    fun getProfiles(): MutableList<ProfileModel> {
        val jsonStr = prefs.getString(KEY_PROFILES_JSON, null) ?: return mutableListOf()
        val list = mutableListOf<ProfileModel>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProfileModel(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        color = obj.getInt("color"),
                        avatarEmoji = obj.optString("avatarEmoji", ""),
                        isGuest = obj.optBoolean("isGuest", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveProfiles(profiles: List<ProfileModel>) {
        val array = JSONArray()
        for (p in profiles) {
            if (p.isGuest) continue // Do not persist guest profile
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("color", p.color)
                put("avatarEmoji", p.avatarEmoji)
                put("isGuest", p.isGuest)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PROFILES_JSON, array.toString()).apply()
    }

    fun getActiveProfile(): ProfileModel {
        val activeId = settings.activeProfileId
        if (activeId == "profile_guest") {
            return ProfileModel("profile_guest", "Guest", 0xFF607D8B.toInt(), "🕶️", isGuest = true)
        }
        val profiles = getProfiles()
        return profiles.find { it.id == activeId } ?: profiles.firstOrNull() ?: ProfileModel("profile_personal", "Personal", PRESET_COLORS[0])
    }

    fun setActiveProfile(id: String) {
        settings.activeProfileId = id
    }

    fun addProfile(name: String, color: Int, avatarEmoji: String = ""): ProfileModel {
        val id = "profile_${System.currentTimeMillis()}"
        val newProfile = ProfileModel(id, name, color, avatarEmoji)
        val list = getProfiles()
        list.add(newProfile)
        saveProfiles(list)
        return newProfile
    }

    fun updateProfile(id: String, name: String, color: Int, avatarEmoji: String): Boolean {
        val list = getProfiles()
        val target = list.find { it.id == id } ?: return false
        target.name = name
        target.color = color
        target.avatarEmoji = avatarEmoji
        saveProfiles(list)
        return true
    }

    fun createGuestProfile(): ProfileModel {
        return ProfileModel("profile_guest", "Guest", 0xFF607D8B.toInt(), "🕶️", isGuest = true)
    }

    fun deleteProfile(id: String): Boolean {
        if (id == "profile_guest") {
            if (settings.activeProfileId == id) {
                settings.activeProfileId = getProfiles().firstOrNull()?.id ?: "profile_personal"
            }
            return true
        }
        val list = getProfiles()
        if (list.size <= 1) return false // Cannot delete last profile

        val target = list.find { it.id == id } ?: return false
        list.remove(target)
        saveProfiles(list)

        // Clear saved tabs
        prefs.edit().remove(KEY_PREFIX_TABS + id).apply()

        if (settings.activeProfileId == id) {
            settings.activeProfileId = list.first().id
        }
        return true
    }

    fun saveProfileTabs(profileId: String, urls: List<String>) {
        val array = JSONArray()
        urls.forEach { array.put(it) }
        prefs.edit().putString(KEY_PREFIX_TABS + profileId, array.toString()).apply()
    }

    fun getProfileTabs(profileId: String): List<String> {
        val jsonStr = prefs.getString(KEY_PREFIX_TABS + profileId, null) ?: return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }
}
