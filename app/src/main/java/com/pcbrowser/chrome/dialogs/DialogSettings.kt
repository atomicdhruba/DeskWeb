package com.pcbrowser.chrome.dialogs

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.pcbrowser.chrome.R
import com.pcbrowser.chrome.data.SettingsPreferences
import com.pcbrowser.chrome.search.SearchEngineManager

class DialogSettings(
    private val context: Context,
    private val settings: SettingsPreferences,
    private var currentZoomPercent: Int = 100,
    private val onAction: (Action, Int) -> Unit
) : PopupWindow(context) {

    enum class Action {
        NEW_TAB,
        HISTORY,
        FULLSCREEN,
        ZOOM_CHANGE,
        TOGGLE_DENSITY,
        ORIENTATION_PICKER,
        TOGGLE_NAV_BUTTONS,
        MANAGE_PROFILES,
        TOGGLE_BOOKMARKS_BAR,
        TOGGLE_IMMERSIVE,
        TOGGLE_DESKTOP_UA,
        TOGGLE_AD_BLOCK,
        TOGGLE_HTTPS_ONLY,
        TOGGLE_DNT,
        MANAGE_COOKIES,
        CERT_INFO,
        SEARCH_ENGINES,
        DOWNLOAD_PATH,
        SCREENSHOT,
        PRINT,
        TRANSLATE,
        FIND_IN_PAGE,
        ABOUT,
        EXIT
    }

    private val popupView: View = LayoutInflater.from(context).inflate(R.layout.app_browser_settings, null, false)
    private val menuContainer: LinearLayout = popupView.findViewById(R.id.menuContainer)
    private val zoomContainer: FrameLayout = popupView.findViewById(R.id.zoomContainer)
    private val textZoom: TextView = popupView.findViewById(R.id.btnZoom)

    init {
        contentView = popupView
        width = LinearLayout.LayoutParams.WRAP_CONTENT
        height = LinearLayout.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        setBackgroundDrawable(ColorDrawable(0))
        elevation = 16f
        animationStyle = R.style.AnimationDialogMaterial

        updateZoomLabel()

        popupView.findViewById<TextView>(R.id.btnNewTab).setOnClickListener {
            dismiss()
            onAction(Action.NEW_TAB, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnHistory).setOnClickListener {
            dismiss()
            onAction(Action.HISTORY, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnFullscreen).setOnClickListener {
            dismiss()
            onAction(Action.FULLSCREEN, currentZoomPercent)
        }

        textZoom.setOnClickListener {
            showZoomSubmenu()
        }

        // Density
        val btnDensity = popupView.findViewById<TextView>(R.id.btnDensity)
        val densityLabel = if (settings.density == SettingsPreferences.DENSITY_COMPACT) "Compact" else "Comfortable"
        btnDensity.text = "Density ($densityLabel)"
        btnDensity.setOnClickListener {
            dismiss()
            onAction(Action.TOGGLE_DENSITY, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnOrientation).setOnClickListener {
            dismiss()
            onAction(Action.ORIENTATION_PICKER, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnNavButtons).setOnClickListener {
            dismiss()
            onAction(Action.TOGGLE_NAV_BUTTONS, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnProfiles).setOnClickListener {
            dismiss()
            onAction(Action.MANAGE_PROFILES, currentZoomPercent)
        }

        val btnBookmarksBar = popupView.findViewById<TextView>(R.id.btnBookmarksBar)
        updateCheckmark(btnBookmarksBar, settings.showBookmarksBar)
        btnBookmarksBar.setOnClickListener {
            dismiss()
            onAction(Action.TOGGLE_BOOKMARKS_BAR, currentZoomPercent)
        }

        val btnImmersive = popupView.findViewById<TextView>(R.id.btnImmersiveMode)
        updateCheckmark(btnImmersive, settings.immersiveMode)
        btnImmersive.setOnClickListener {
            dismiss()
            onAction(Action.TOGGLE_IMMERSIVE, currentZoomPercent)
        }

        val btnDesktopUA = popupView.findViewById<TextView>(R.id.btnDesktopUA)
        updateCheckmark(btnDesktopUA, settings.desktopUserAgent)
        btnDesktopUA.setOnClickListener {
            dismiss()
            onAction(Action.TOGGLE_DESKTOP_UA, currentZoomPercent)
        }

        // Privacy options
        val btnAdBlock = popupView.findViewById<TextView>(R.id.btnAdBlock)
        updateCheckmark(btnAdBlock, settings.adBlockEnabled)
        btnAdBlock.setOnClickListener {
            dismiss()
            onAction(Action.TOGGLE_AD_BLOCK, currentZoomPercent)
        }

        val btnHttpsOnly = popupView.findViewById<TextView>(R.id.btnHttpsOnly)
        updateCheckmark(btnHttpsOnly, settings.httpsOnly)
        btnHttpsOnly.setOnClickListener {
            dismiss()
            onAction(Action.TOGGLE_HTTPS_ONLY, currentZoomPercent)
        }

        val btnDnt = popupView.findViewById<TextView>(R.id.btnDnt)
        updateCheckmark(btnDnt, settings.doNotTrack)
        btnDnt.setOnClickListener {
            dismiss()
            onAction(Action.TOGGLE_DNT, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnCookies).setOnClickListener {
            dismiss()
            onAction(Action.MANAGE_COOKIES, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnCertInfo).setOnClickListener {
            dismiss()
            onAction(Action.CERT_INFO, currentZoomPercent)
        }

        // Tools
        val btnSearchEngine = popupView.findViewById<TextView>(R.id.btnSearchEngine)
        val activeEngineName = SearchEngineManager.getEngine(settings.activeSearchEngineId).name
        btnSearchEngine.text = "Search Engine ($activeEngineName)"
        btnSearchEngine.setOnClickListener {
            dismiss()
            onAction(Action.SEARCH_ENGINES, currentZoomPercent)
        }

        val btnDownloadPath = popupView.findViewById<TextView>(R.id.btnDownloadPath)
        btnDownloadPath.text = "Downloads (${settings.downloadPathName})"
        btnDownloadPath.setOnClickListener {
            dismiss()
            onAction(Action.DOWNLOAD_PATH, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnScreenshot).setOnClickListener {
            dismiss()
            onAction(Action.SCREENSHOT, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnPrint).setOnClickListener {
            dismiss()
            onAction(Action.PRINT, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnTranslate).setOnClickListener {
            dismiss()
            onAction(Action.TRANSLATE, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnSearch).setOnClickListener {
            dismiss()
            onAction(Action.FIND_IN_PAGE, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnAbout).setOnClickListener {
            dismiss()
            onAction(Action.ABOUT, currentZoomPercent)
        }

        popupView.findViewById<TextView>(R.id.btnExit).setOnClickListener {
            dismiss()
            onAction(Action.EXIT, currentZoomPercent)
        }
    }

    private fun updateCheckmark(tv: TextView, isChecked: Boolean) {
        if (isChecked) {
            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_checkmark_24_filled, 0, 0, 0)
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    private fun updateZoomLabel() {
        textZoom.text = context.getString(R.string.browser_zoom_value, currentZoomPercent)
    }

    private fun showZoomSubmenu() {
        menuContainer.visibility = View.GONE
        zoomContainer.visibility = View.VISIBLE
        zoomContainer.removeAllViews()

        val zoomView = LayoutInflater.from(context).inflate(R.layout.app_browser_settings_zoom, zoomContainer, false)
        val zoomText = zoomView.findViewById<TextView>(R.id.zoom_value_text)
        val btnPlus = zoomView.findViewById<ImageButton>(R.id.btn_zoom_plus)
        val btnMinus = zoomView.findViewById<ImageButton>(R.id.btn_zoom_minus)
        val btnReset = zoomView.findViewById<ImageButton>(R.id.btn_zoom_reset)
        val btnFullscreen = zoomView.findViewById<ImageButton>(R.id.btn_zoom_fullscreen)
        val btnBack = zoomView.findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener {
            zoomContainer.visibility = View.GONE
            menuContainer.visibility = View.VISIBLE
        }

        btnFullscreen?.setOnClickListener {
            dismiss()
            onAction(Action.FULLSCREEN, currentZoomPercent)
        }

        zoomText.text = context.getString(R.string.browser_zoom_value_sm, currentZoomPercent)

        btnPlus.setOnClickListener {
            if (currentZoomPercent < 300) {
                currentZoomPercent += 10
                zoomText.text = context.getString(R.string.browser_zoom_value_sm, currentZoomPercent)
                updateZoomLabel()
                onAction(Action.ZOOM_CHANGE, currentZoomPercent)
            }
        }

        btnMinus.setOnClickListener {
            if (currentZoomPercent > 50) {
                currentZoomPercent -= 10
                zoomText.text = context.getString(R.string.browser_zoom_value_sm, currentZoomPercent)
                updateZoomLabel()
                onAction(Action.ZOOM_CHANGE, currentZoomPercent)
            }
        }

        btnReset.setOnClickListener {
            currentZoomPercent = 100
            zoomText.text = context.getString(R.string.browser_zoom_value_sm, currentZoomPercent)
            updateZoomLabel()
            onAction(Action.ZOOM_CHANGE, currentZoomPercent)
        }

        zoomContainer.addView(zoomView)
    }

    fun show(anchor: View) {
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val xOffset = -(popupView.measuredWidth - anchor.width)
        showAsDropDown(anchor, xOffset, 0)
    }
}
