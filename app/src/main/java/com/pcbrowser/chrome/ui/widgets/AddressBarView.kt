package com.pcbrowser.chrome.ui.widgets

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.util.AttributeSet
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
