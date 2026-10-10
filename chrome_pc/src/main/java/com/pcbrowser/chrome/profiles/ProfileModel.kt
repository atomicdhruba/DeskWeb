package com.pcbrowser.chrome.profiles

data class ProfileModel(
    val id: String,
    var name: String,
    var color: Int,
    var avatarEmoji: String = "",
    val isGuest: Boolean = false
) {
    val initial: String
        get() = if (avatarEmoji.isNotEmpty()) avatarEmoji else if (name.isNotEmpty()) name.substring(0, 1).uppercase() else "C"
}
