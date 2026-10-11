package com.pcbrowser.chrome.dialogs

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import com.pcbrowser.chrome.R

class UIFinder(
    private val context: Context,
    private val overlayContainer: FrameLayout,
    private val getActiveWebView: () -> WebView?
) {

    private var finderView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    fun show() {
        if (finderView == null) {
            val view = LayoutInflater.from(context).inflate(R.layout.app_browser_dialog_finder, overlayContainer, false)
            finderView = view

            val editSearch = view.findViewById<EditText>(R.id.edit_app_name)
            val btnNext = view.findViewById<ImageButton>(R.id.btn_next)
            val btnPrev = view.findViewById<ImageButton>(R.id.btn_prev)
            val btnClose = view.findViewById<ImageButton>(R.id.btn_close)

            editSearch.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    searchRunnable?.let { handler.removeCallbacks(it) }
                    val query = s?.toString() ?: ""
                    searchRunnable = Runnable {
                        val webView = getActiveWebView() ?: return@Runnable
                        if (query.isEmpty()) {
                            webView.clearMatches()
                        } else {
                            webView.findAllAsync(query)
                        }
                    }
                    handler.postDelayed(searchRunnable!!, 250)
                }
            })

            editSearch.setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                    if (keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                        getActiveWebView()?.findNext(!event.isShiftPressed)
                        return@setOnKeyListener true
                    }
                    if (keyCode == android.view.KeyEvent.KEYCODE_ESCAPE) {
                        hide()
                        return@setOnKeyListener true
                    }
                }
                false
            }

            btnNext.setOnClickListener {
                getActiveWebView()?.findNext(true)
            }

            btnPrev.setOnClickListener {
                getActiveWebView()?.findNext(false)
            }

            btnClose.setOnClickListener {
                hide()
            }
        }

        val view = finderView ?: return
        if (view.parent == null) {
            overlayContainer.removeAllViews()
            overlayContainer.addView(view)
        }
        overlayContainer.visibility = View.VISIBLE
        val animOpen = AnimationUtils.loadAnimation(context, R.anim.ui_browser_dialog_open)
        overlayContainer.startAnimation(animOpen)

        val editSearch = view.findViewById<EditText>(R.id.edit_app_name)
        editSearch.requestFocus()
    }

    fun hide() {
        val webView = getActiveWebView()
        webView?.clearMatches()

        if (overlayContainer.visibility == View.VISIBLE) {
            val animClose = AnimationUtils.loadAnimation(context, R.anim.ui_browser_dialog_close)
            overlayContainer.startAnimation(animClose)
            overlayContainer.visibility = View.GONE
            overlayContainer.removeAllViews()
            finderView = null
        }
    }

    fun isVisible(): Boolean = overlayContainer.visibility == View.VISIBLE
}
