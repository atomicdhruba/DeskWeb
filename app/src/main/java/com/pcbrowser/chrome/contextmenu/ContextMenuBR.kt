package com.pcbrowser.chrome.contextmenu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import com.pcbrowser.chrome.MainActivity
import com.pcbrowser.chrome.R
import com.pcbrowser.chrome.download.Downloader
import com.pcbrowser.chrome.ui.widgets.ShortcutWebView
import com.pcbrowser.chrome.util.ScreenshotHelper

object ContextMenuBR {

    fun show(
        activity: MainActivity,
        webView: WebView,
        hitTestResult: WebView.HitTestResult,
        clickX: Float = 0f,
        clickY: Float = 0f
    ) {
        val popup = PopupMenu(activity, webView)
        val menu = popup.menu

        val type = hitTestResult.type
        val extra = hitTestResult.extra

        var order = 0

        // 1. Link items
        if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            val linkUrl = extra
            if (!linkUrl.isNullOrEmpty()) {
                menu.add(0, 101, order++, R.string.context_menu_open_link_new_tab)
                menu.add(0, 102, order++, "Open link in background tab")
                menu.add(0, 103, order++, R.string.context_menu_copy_link_address)
            }
        }

        // 2. Image items
        if (type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            val imgUrl = extra
            if (!imgUrl.isNullOrEmpty()) {
                menu.add(0, 201, order++, R.string.context_menu_open_img_new_tab)
                menu.add(0, 202, order++, R.string.context_menu_save_img_as)
                menu.add(0, 203, order++, R.string.context_menu_copy_img_address)
            }
        }

        // 3. Editable / Text fields
        if (type == WebView.HitTestResult.EDIT_TEXT_TYPE) {
            menu.add(0, 301, order++, "Cut")
            menu.add(0, 302, order++, "Copy")
            menu.add(0, 303, order++, "Paste")
            menu.add(0, 304, order++, "Select All")
        }

        // 4. Standard Desktop Navigation & Page Actions (always present on desktop right click)
        val itemBack = menu.add(0, 401, order++, R.string.nav_go_back)
        itemBack.isEnabled = webView.canGoBack()

        val itemFwd = menu.add(0, 402, order++, R.string.nav_go_forward)
        itemFwd.isEnabled = webView.canGoForward()

        menu.add(0, 403, order++, R.string.nav_reload)
        menu.add(0, 404, order++, R.string.context_menu_print)
        menu.add(0, 405, order++, R.string.browser_ct_menu_find)
        menu.add(0, 406, order++, R.string.browser_menu_screenshot)
        menu.add(0, 407, order++, "View Page Source")

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                101 -> {
                    extra?.let { activity.openNewTab(it, selectNow = true) }
                    true
                }
                102 -> {
                    extra?.let { activity.openNewTab(it, selectNow = false) }
                    Toast.makeText(activity, "Opened in background tab", Toast.LENGTH_SHORT).show()
                    true
                }
                103 -> {
                    extra?.let { copyToClipboard(activity, "Link", it, "Link address copied") }
                    true
                }
                201 -> {
                    extra?.let { activity.openNewTab(it, selectNow = true) }
                    true
                }
                202 -> {
                    extra?.let { activity.saveImage(it) }
                    true
                }
                203 -> {
                    extra?.let { copyToClipboard(activity, "Image URL", it, "Image address copied") }
                    true
                }
                301 -> {
                    webView.evaluateJavascript("document.execCommand('cut')", null)
                    true
                }
                302 -> {
                    webView.evaluateJavascript("document.execCommand('copy')", null)
                    true
                }
                303 -> {
                    if (webView is ShortcutWebView) {
                        webView.pasteClipboard()
                    }
                    true
                }
                304 -> {
                    webView.evaluateJavascript("document.execCommand('selectAll')", null)
                    true
                }
                401 -> {
                    if (webView.canGoBack()) webView.goBack()
                    true
                }
                402 -> {
                    if (webView.canGoForward()) webView.goForward()
                    true
                }
                403 -> {
                    webView.reload()
                    true
                }
                404 -> {
                    activity.printCurrentPage()
                    true
                }
                405 -> {
                    activity.showFindInPage()
                    true
                }
                406 -> {
                    ScreenshotHelper.captureAndShare(activity, webView)
                    true
                }
                407 -> {
                    val currentUrl = webView.url ?: ""
                    if (currentUrl.isNotEmpty()) {
                        activity.openNewTab("view-source:$currentUrl", selectNow = true)
                    }
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun copyToClipboard(context: Context, label: String, text: String, feedback: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
    }
}
