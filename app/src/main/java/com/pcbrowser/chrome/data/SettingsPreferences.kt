package com.pcbrowser.chrome.data

import android.content.Context
import android.content.SharedPreferences

class SettingsPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "deskweb_settings"

        const val KEY_ORIENTATION = "key_orientation"
        const val ORIENTATION_LANDSCAPE = "landscape"
        const val ORIENTATION_PORTRAIT = "portrait"
        const val ORIENTATION_AUTO = "sensor"

        const val KEY_HIDE_NAV_BUTTONS = "key_hide_nav_buttons"
        const val KEY_SHOW_BOOKMARKS_BAR = "key_show_bookmarks_bar"
        const val KEY_DESKTOP_UA = "key_desktop_ua"
        const val KEY_ACTIVE_PROFILE_ID = "key_active_profile_id"
        const val KEY_IMMERSIVE_MODE = "key_immersive_mode"

        const val KEY_AD_BLOCK = "key_ad_block"
        const val KEY_HTTPS_ONLY = "key_https_only"
        const val KEY_DO_NOT_TRACK = "key_do_not_track"

        const val KEY_DENSITY = "key_density"
        const val DENSITY_COMFORTABLE = "comfortable"
        const val DENSITY_COMPACT = "compact"

        const val KEY_DOWNLOAD_PATH = "key_download_path"
        const val KEY_DOWNLOAD_PATH_NAME = "key_download_path_name"

        const val KEY_SEARCH_ENGINE = "key_search_engine"
    }

    var orientation: String
        get() = prefs.getString(KEY_ORIENTATION, ORIENTATION_LANDSCAPE) ?: ORIENTATION_LANDSCAPE
        set(value) = prefs.edit().putString(KEY_ORIENTATION, value).apply()

    var hideNavButtons: Boolean
        get() = prefs.getBoolean(KEY_HIDE_NAV_BUTTONS, true) // default true as requested by user
        set(value) = prefs.edit().putBoolean(KEY_HIDE_NAV_BUTTONS, value).apply()

    var showBookmarksBar: Boolean
        get() = prefs.getBoolean(KEY_SHOW_BOOKMARKS_BAR, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_BOOKMARKS_BAR, value).apply()

    var desktopUserAgent: Boolean
        get() = prefs.getBoolean(KEY_DESKTOP_UA, true)
        set(value) = prefs.edit().putBoolean(KEY_DESKTOP_UA, value).apply()

    var activeProfileId: String
        get() = prefs.getString(KEY_ACTIVE_PROFILE_ID, "profile_personal") ?: "profile_personal"
        set(value) = prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, value).apply()

    var immersiveMode: Boolean
        get() = prefs.getBoolean(KEY_IMMERSIVE_MODE, true) // default true (fullscreen system bars hidden)
        set(value) = prefs.edit().putBoolean(KEY_IMMERSIVE_MODE, value).apply()

    var adBlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_AD_BLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_AD_BLOCK, value).apply()

    var httpsOnly: Boolean
        get() = prefs.getBoolean(KEY_HTTPS_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_HTTPS_ONLY, value).apply()

    var doNotTrack: Boolean
        get() = prefs.getBoolean(KEY_DO_NOT_TRACK, true)
        set(value) = prefs.edit().putBoolean(KEY_DO_NOT_TRACK, value).apply()

    var density: String
        get() = prefs.getString(KEY_DENSITY, DENSITY_COMFORTABLE) ?: DENSITY_COMFORTABLE
        set(value) = prefs.edit().putString(KEY_DENSITY, value).apply()

    var downloadPath: String
        get() = prefs.getString(KEY_DOWNLOAD_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DOWNLOAD_PATH, value).apply()

    var downloadPathName: String
        get() = prefs.getString(KEY_DOWNLOAD_PATH_NAME, "Downloads") ?: "Downloads"
        set(value) = prefs.edit().putString(KEY_DOWNLOAD_PATH_NAME, value).apply()

    var activeSearchEngineId: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, "google") ?: "google"
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()
}
