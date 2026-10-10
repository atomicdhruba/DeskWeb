package com.pcbrowser.chrome.engine

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

object WebMount {

    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebSettings(webView: WebView, context: Context, zoomPercent: Int = 100, isDesktop: Boolean = true) {
        val settings = webView.settings
        AdBlocker.init(context)
        setUserAgent(webView, context, isDesktop)

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true

        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mediaPlaybackRequiresUserGesture = false

        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.builtInZoomControls = true
        settings.setSupportZoom(true)
        settings.displayZoomControls = false

        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            settings.forceDark = WebSettings.FORCE_DARK_AUTO
        }

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        applyZoom(webView, zoomPercent)
    }

    fun applyZoom(webView: WebView, zoomPercent: Int) {
        if (zoomPercent != 100) {
            webView.evaluateJavascript("document.body.style.zoom = '${zoomPercent}%';", null)
        } else {
            webView.evaluateJavascript("document.body.style.zoom = '100%';", null)
        }
    }

    fun setUserAgent(webView: WebView, context: Context, isDesktop: Boolean) {
        val defaultUa = WebSettings.getDefaultUserAgent(context)
        if (isDesktop) {
            val desktopUa = defaultUa
                .replace("Android", "X11; Linux x86_64")
                .replace("; Mobile", "")
                .replace("Mobile Safari", "Safari")
            webView.settings.userAgentString = desktopUa
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
        } else {
            webView.settings.userAgentString = defaultUa
            webView.settings.useWideViewPort = false
            webView.settings.loadWithOverviewMode = false
        }
    }
}
