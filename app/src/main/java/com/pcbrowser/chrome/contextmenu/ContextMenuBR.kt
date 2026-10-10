package com.pcbrowser.chrome.contextmenu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import com.pcbrowser.chrome.R

object ContextMenuBR {

    interface ContextMenuCallback {
        fun onOpenInNewTab(url: String)
        fun onSaveImage(url: String)
    }

    fun show(
        context: Context,
        anchor: View,
        hitTestResult: WebView.HitTestResult,
        callback: ContextMenuCallback
    ) {
        val type = hitTestResult.type
        val extra = hitTestResult.extra ?: return

        val popup = PopupMenu(context, anchor)
        val menu = popup.menu

        when (type) {
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                menu.add(0, 1, 0, R.string.context_menu_open_link_new_tab)
                menu.add(0, 2, 1, R.string.context_menu_copy_link_address)
            }
            WebView.HitTestResult.IMAGE_TYPE -> {
                menu.add(0, 3, 0, R.string.context_menu_open_img_new_tab)
                menu.add(0, 4, 1, R.string.context_menu_save_img_as)
                menu.add(0, 5, 2, R.string.context_menu_copy_img_address)
            }
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                menu.add(0, 1, 0, R.string.context_menu_open_link_new_tab)
                menu.add(0, 2, 1, R.string.context_menu_copy_link_address)
                menu.add(0, 3, 2, R.string.context_menu_open_img_new_tab)
                menu.add(0, 4, 3, R.string.context_menu_save_img_as)
                menu.add(0, 5, 4, R.string.context_menu_copy_img_address)
            }
            else -> return
        }

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                1 -> {
                    callback.onOpenInNewTab(extra)
                    true
                }
                2 -> {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Link", extra))
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                    true
                }
                3 -> {
                    callback.onOpenInNewTab(extra)
                    true
                }
                4 -> {
                    callback.onSaveImage(extra)
                    true
                }
                5 -> {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Image URL", extra))
                    Toast.makeText(context, "Image address copied", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }
}
