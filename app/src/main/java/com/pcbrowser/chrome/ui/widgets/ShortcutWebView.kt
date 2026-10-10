package com.pcbrowser.chrome.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.webkit.WebView

class ShortcutWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    interface KeyShortcutListener {
        fun onShortcut(keyCode: Int, event: KeyEvent): Boolean
    }

    var keyShortcutListener: KeyShortcutListener? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.isCtrlPressed) {
            val handled = keyShortcutListener?.onShortcut(event.keyCode, event) ?: false
            if (handled) return true
        }
        return super.dispatchKeyEvent(event)
    }
}
