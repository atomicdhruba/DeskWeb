package com.pcbrowser.chrome.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import android.net.http.SslCertificate
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.pcbrowser.chrome.MainActivity
import com.pcbrowser.chrome.R
import com.pcbrowser.chrome.contextmenu.ContextMenuBR
import com.pcbrowser.chrome.download.Downloader
import com.pcbrowser.chrome.engine.AdBlocker
import com.pcbrowser.chrome.engine.WebMount
import com.pcbrowser.chrome.ui.widgets.ShortcutWebView

class BrowserTabFragment : Fragment() {

    var webView: ShortcutWebView? = null
        private set

    private var tabId: String = ""
    private var currentUrl: String = "https://www.google.com"
    private var currentZoomPercent: Int = 100
    var currentCertificate: SslCertificate? = null
        private set

    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    private var customVideoView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    companion object {
        private const val ARG_TAB_ID = "arg_tab_id"
        private const val ARG_URL = "arg_url"

        fun newInstance(tabId: String, url: String): BrowserTabFragment {
            val fragment = BrowserTabFragment()
            val args = Bundle().apply {
                putString(ARG_TAB_ID, tabId)
                putString(ARG_URL, url)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tabId = it.getString(ARG_TAB_ID, "")
            currentUrl = it.getString(ARG_URL, "https://www.google.com")
        }

        fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            var results: Array<Uri>? = null
            if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
                val dataString = data.dataString
                val clipData = data.clipData
                if (clipData != null) {
                    results = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                } else if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
            fileChooserCallback?.onReceiveValue(results)
            fileChooserCallback = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.app_browser_tab, container, false)
        val wv = view.findViewById<ShortcutWebView>(R.id.webView)
        webView = wv

        val mainActivity = activity as? MainActivity
        val isDesktop = mainActivity?.isDesktopUaEnabled() ?: true
        WebMount.configureWebSettings(wv, requireContext(), currentZoomPercent, isDesktop)

        // Setup Downloader
        mainActivity?.let {
            Downloader.setup(requireContext(), wv, it.getDownloadsPopup(), it.getDownloadsButton())
        }

        // Setup Shortcut forwarding
        wv.isKeyboardRestricted = mainActivity?.isKeyboardRestricted() ?: false
        wv.keyShortcutListener = object : ShortcutWebView.KeyShortcutListener {
            override fun onShortcut(keyCode: Int, event: KeyEvent): Boolean {
                return mainActivity?.handleShortcut(keyCode) ?: false
            }
        }

        // Right-Click & Long-Press context menu
        wv.setOnGenericMotionListener { _, motionEvent ->
            if ((motionEvent.buttonState and MotionEvent.BUTTON_SECONDARY) != 0 &&
                motionEvent.action == MotionEvent.ACTION_BUTTON_PRESS
            ) {
                showContextMenu(wv)
                return@setOnGenericMotionListener true
            }
            false
        }

        wv.setOnLongClickListener {
            showContextMenu(wv)
            true
        }

        // Chrome and Web Clients
        setupClients(wv, mainActivity)

        wv.loadUrl(currentUrl)
        return view
    }

    private fun showContextMenu(wv: WebView) {
        val hitTestResult = wv.hitTestResult
        val mainActivity = activity as? MainActivity ?: return
        ContextMenuBR.show(requireContext(), wv, hitTestResult, object : ContextMenuBR.ContextMenuCallback {
            override fun onOpenInNewTab(url: String) {
                mainActivity.openNewTab(url)
            }

            override fun onSaveImage(url: String) {
                mainActivity.saveImage(url)
            }
        })
    }

    private fun setupClients(wv: ShortcutWebView, mainActivity: MainActivity?) {
        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                mainActivity?.onTabProgress(tabId, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                mainActivity?.onTabTitle(tabId, title ?: "New tab")
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                mainActivity?.onTabFavicon(tabId, icon)
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val tempWebView = WebView(requireContext())
                tempWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        url?.let { mainActivity?.openNewTab(it) }
                        tempWebView.destroy()
                        return true
                    }
                }
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = tempWebView
                resultMsg?.sendToTarget()
                return true
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customVideoView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customVideoView = view
                customViewCallback = callback
                mainActivity?.showFullscreenVideo(view)
            }

            override fun onHideCustomView() {
                mainActivity?.hideFullscreenVideo(customVideoView)
                customVideoView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                try {
                    fileChooserLauncher.launch(intent)
                } catch (e: Exception) {
                    fileChooserCallback = null
                    return false
                }
                return true
            }
        }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUrl = request?.url?.toString()
                if (mainActivity?.isAdBlockEnabled() == true && AdBlocker.isAd(reqUrl)) {
                    return AdBlocker.createEmptyResource()
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                currentCertificate = null
                url?.let {
                    currentUrl = it
                    mainActivity?.onTabUrl(tabId, it, true)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                currentCertificate = view?.certificate
                if (wv.isKeyboardRestricted) {
                    wv.applyKeyboardRestrictionJs(true)
                }
                url?.let {
                    currentUrl = it
                    mainActivity?.onTabUrl(tabId, it, false)
                    mainActivity?.recordHistory(it, view?.title ?: it)
                }
                // Inject Blob download interceptor
                wv.evaluateJavascript(Downloader.BLOB_INTERCEPTOR_SCRIPT, null)
                WebMount.applyZoom(wv, currentZoomPercent)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false
                if (mainActivity?.isHttpsOnlyEnabled() == true && url.startsWith("http://", ignoreCase = true) && !url.startsWith("http://localhost", ignoreCase = true)) {
                    val httpsUrl = url.replaceFirst("http://", "https://", ignoreCase = true)
                    loadUrl(httpsUrl)
                    return true
                }
                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    } catch (_: Exception) {}
                }
                return false
            }
        }
    }

    fun loadUrl(url: String) {
        currentUrl = url
        val mainActivity = activity as? MainActivity
        val headers = if (mainActivity?.isDoNotTrackEnabled() == true) {
            mapOf("DNT" to "1")
        } else {
            emptyMap()
        }
        if (headers.isNotEmpty()) {
            webView?.loadUrl(url, headers)
        } else {
            webView?.loadUrl(url)
        }
    }

    fun canGoBack(): Boolean = webView?.canGoBack() ?: false
    fun goBack() = webView?.goBack()

    fun canGoForward(): Boolean = webView?.canGoForward() ?: false
    fun goForward() = webView?.goForward()

    fun reload() = webView?.reload()
    fun stopLoading() = webView?.stopLoading()

    fun setZoom(percent: Int) {
        currentZoomPercent = percent
        webView?.let { WebMount.applyZoom(it, percent) }
    }

    fun updateDesktopMode(isDesktop: Boolean) {
        val wv = webView ?: return
        val ctx = context ?: return
        WebMount.setUserAgent(wv, ctx, isDesktop)
        wv.reload()
    }

    override fun onDestroyView() {
        webView?.stopLoading()
        webView?.webChromeClient = null
        webView?.destroy()
        webView = null
        super.onDestroyView()
    }
}
