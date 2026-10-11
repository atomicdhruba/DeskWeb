package com.pcbrowser.chrome.ui.widgets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

class AddressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.autoCompleteTextViewStyle
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    private var rawAddress: String = ""
    private var isUserEditing: Boolean = false

    init {
        setSingleLine(true)
        setOnClickListener {
            if (!isUserEditing) {
                setText(rawAddress)
                selectAll()
                isUserEditing = true
            }
        }
    }

    fun setAddress(url: String) {
        rawAddress = url
        setText(formatDisplayUrl(url))
        isUserEditing = false
    }

    fun getRawAddress(): String = rawAddress

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) {
            post {
                setText(rawAddress)
                selectAll()
            }
        } else {
            setText(formatDisplayUrl(rawAddress))
            setSelection(0)
            isUserEditing = false
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            setText(formatDisplayUrl(rawAddress))
            clearFocus()
            return true
        }

        val isCopy = event.keyCode == KeyEvent.KEYCODE_COPY || (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_C)
        val isPaste = event.keyCode == KeyEvent.KEYCODE_PASTE || (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_V)
        val isCut = event.keyCode == KeyEvent.KEYCODE_CUT || (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_X)
        val isSelectAll = event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_A

        if (event.action == KeyEvent.ACTION_DOWN) {
            when {
                isCopy -> {
                    copyToClipboard()
                    return true
                }
                isPaste -> {
                    pasteFromClipboard()
                    return true
                }
                isCut -> {
                    cutToClipboard()
                    return true
                }
                isSelectAll -> {
                    selectAll()
                    return true
                }
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (isCopy || isPaste || isCut || isSelectAll) {
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    fun copyToClipboard() {
        val start = selectionStart
        val end = selectionEnd
        val selectedText = if (start >= 0 && end >= 0 && start != end) {
            val min = minOf(start, end)
            val max = maxOf(start, end)
            text?.substring(min, max) ?: ""
        } else {
            text?.toString() ?: ""
        }
        if (selectedText.isNotEmpty()) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            cm?.setPrimaryClip(android.content.ClipData.newPlainText("URL", selectedText))
        }
    }

    fun pasteFromClipboard() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = cm?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pasteText = clip.getItemAt(0).coerceToText(context).toString()
            val start = selectionStart
            val end = selectionEnd
            val editable = text
            if (editable != null) {
                if (start >= 0 && end >= 0 && start != end) {
                    val min = minOf(start, end)
                    val max = maxOf(start, end)
                    editable.replace(min, max, pasteText)
                } else if (start >= 0) {
                    editable.insert(start, pasteText)
                } else {
                    editable.append(pasteText)
                }
                isUserEditing = true
            }
        }
    }

    fun cutToClipboard() {
        copyToClipboard()
        val start = selectionStart
        val end = selectionEnd
        val editable = text
        if (editable != null && start >= 0 && end >= 0 && start != end) {
            val min = minOf(start, end)
            val max = maxOf(start, end)
            editable.delete(min, max)
            isUserEditing = true
        }
    }

    companion object {
        fun formatDisplayUrl(url: String?): String {
            if (url.isNullOrEmpty()) return ""
            return try {
                val uri = Uri.parse(url)
                val host = uri.host
                val path = uri.path
                val query = uri.query

                if ("www.google.com".equals(host, ignoreCase = true) &&
                    (path.isNullOrEmpty() || "/" == path || "/webhp".equals(path, ignoreCase = true))
                ) {
                    return ""
                }

                var cleanHost = host
                if (cleanHost != null && cleanHost.startsWith("www.")) {
                    cleanHost = cleanHost.substring(4)
                }

                val sb = StringBuilder()
                if (cleanHost != null) sb.append(cleanHost)
                if (!path.isNullOrEmpty() && "/" != path) sb.append(path)
                if (!query.isNullOrEmpty()) {
                    sb.append("?").append(query)
                }
                sb.toString()
            } catch (e: Exception) {
                url
            }
        }
    }
}
