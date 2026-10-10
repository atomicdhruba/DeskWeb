package com.pcbrowser.chrome.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import com.pcbrowser.chrome.R
import com.pcbrowser.chrome.profiles.ProfileManager
import com.pcbrowser.chrome.profiles.ProfileModel

class DialogProfile(
    private val context: Context,
    private val profileManager: ProfileManager,
    private val onProfileSwitched: (ProfileModel) -> Unit
) : PopupWindow(context) {

    private val popupView: View = LayoutInflater.from(context).inflate(R.layout.app_browser_dialog_profile, null, false)
    private val activeAvatarBadge: TextView = popupView.findViewById(R.id.activeAvatarBadge)
    private val activeProfileName: TextView = popupView.findViewById(R.id.activeProfileName)
    private val profileListContainer: LinearLayout = popupView.findViewById(R.id.profileListContainer)
    private val btnAddProfile: LinearLayout = popupView.findViewById(R.id.btnAddProfile)

    init {
        contentView = popupView
        width = LinearLayout.LayoutParams.WRAP_CONTENT
        height = LinearLayout.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        setBackgroundDrawable(ColorDrawable(0))
        animationStyle = R.style.AnimationDialogMaterial

        refreshUI()

        btnAddProfile.setOnClickListener {
            dismiss()
            showAddProfileDialog()
        }

        popupView.findViewById<View>(R.id.btnGuestMode)?.setOnClickListener {
            dismiss()
            val guestProfile = profileManager.createGuestProfile()
            profileManager.setActiveProfile(guestProfile.id)
            onProfileSwitched(guestProfile)
            Toast.makeText(context, "Switched to Guest Mode (Private)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshUI() {
        val activeProfile = profileManager.getActiveProfile()
        activeProfileName.text = activeProfile.name
        activeAvatarBadge.text = activeProfile.initial
        setCircleColor(activeAvatarBadge, activeProfile.color)

        profileListContainer.removeAllViews()
        val allProfiles = profileManager.getProfiles()

        for (profile in allProfiles) {
            val itemView = LayoutInflater.from(context).inflate(R.layout.app_browser_profile_item, profileListContainer, false)
            val badge = itemView.findViewById<TextView>(R.id.avatarBadge)
            val name = itemView.findViewById<TextView>(R.id.profileName)
            val check = itemView.findViewById<ImageView>(R.id.imgActiveCheck)
            val btnDelete = itemView.findViewById<ImageButton>(R.id.btnDeleteProfile)

            badge.text = profile.initial
            setCircleColor(badge, profile.color)
            name.text = profile.name

            val isCurrent = profile.id == activeProfile.id
            if (isCurrent) {
                check.visibility = View.VISIBLE
                btnDelete.visibility = View.GONE
            } else {
                check.visibility = View.GONE
                if (allProfiles.size > 1) {
                    btnDelete.visibility = View.VISIBLE
                    btnDelete.setOnClickListener {
                        showDeleteConfirmDialog(profile)
                    }
                }
            }

            itemView.setOnClickListener {
                if (!isCurrent) {
                    profileManager.setActiveProfile(profile.id)
                    dismiss()
                    onProfileSwitched(profile)
                }
            }

            profileListContainer.addView(itemView)
        }
    }

    private fun setCircleColor(view: View, color: Int) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        view.background = drawable
    }

    private fun showAddProfileDialog() {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }

        val input = EditText(context).apply {
            hint = "Profile Name (e.g. Work, School, Gaming)"
            setSingleLine(true)
        }
        container.addView(input)

        val emojiLabel = TextView(context).apply {
            text = "Choose Profile Avatar:"
            textSize = 12f
            setPadding(0, 20, 0, 10)
        }
        container.addView(emojiLabel)

        val emojiScroll = android.widget.HorizontalScrollView(context).apply {
            isFillViewport = true
        }
        val emojiRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val emojis = listOf("👤", "💼", "🎮", "📚", "🛒", "🎨", "🚀", "💻", "🐱", "⭐", "🍕", "⚡")
        var selectedEmoji = "👤"
        val emojiViews = mutableListOf<TextView>()

        for (em in emojis) {
            val tv = TextView(context).apply {
                text = em
                textSize = 24f
                setPadding(16, 12, 16, 12)
                isClickable = true
                isFocusable = true
                if (em == selectedEmoji) {
                    setBackgroundColor(0x331A73E8)
                }
                setOnClickListener {
                    selectedEmoji = em
                    emojiViews.forEach { v -> v.setBackgroundColor(0) }
                    setBackgroundColor(0x331A73E8)
                }
            }
            emojiViews.add(tv)
            emojiRow.addView(tv)
        }
        emojiScroll.addView(emojiRow)
        container.addView(emojiScroll)

        AlertDialog.Builder(context)
            .setTitle("Add Profile")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val existingCount = profileManager.getProfiles().size
                    val color = ProfileManager.PRESET_COLORS[existingCount % ProfileManager.PRESET_COLORS.size]
                    val newProfile = profileManager.addProfile(name, color, selectedEmoji)
                    profileManager.setActiveProfile(newProfile.id)
                    onProfileSwitched(newProfile)
                    Toast.makeText(context, "Switched to profile: ${newProfile.name}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmDialog(profile: ProfileModel) {
        AlertDialog.Builder(context)
            .setTitle("Delete Profile")
            .setMessage("Are you sure you want to delete '${profile.name}' and all its saved tabs?")
            .setPositiveButton("Delete") { _, _ ->
                if (profileManager.deleteProfile(profile.id)) {
                    refreshUI()
                    Toast.makeText(context, "Profile deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun show(anchor: View) {
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val xOffset = -(popupView.measuredWidth - anchor.width)
        showAsDropDown(anchor, xOffset, 0)
    }
}
