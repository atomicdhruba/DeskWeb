package com.pcbrowser.chrome.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.view.View
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.Toast
import com.pcbrowser.chrome.R
import com.pcbrowser.chrome.data.SettingsPreferences
import com.pcbrowser.chrome.dialogs.UIDownloads
import java.io.File
import java.io.FileOutputStream

object Downloader {

    fun setup(
        context: Context,
        webView: WebView,
        downloadsPopup: UIDownloads?,
        anchorButton: View?
    ) {
        // Blob / Data URL Base64 downloader interface
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun downloadBase64File(base64Data: String, fileName: String) {
                Thread {
                    try {
                        val cleanBase64 = if (base64Data.contains(",")) {
                            base64Data.substring(base64Data.indexOf(",") + 1)
                        } else {
                            base64Data
                        }
                        val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs()
                        }
                        val outputFile = File(downloadsDir, fileName)
                        FileOutputStream(outputFile).use { fos ->
                            fos.write(bytes)
                            fos.flush()
                        }
                        anchorButton?.let { anchor ->
                            downloadsPopup?.showDownloadStatus(anchor, fileName, 2)
                        }
                    } catch (e: Exception) {
                        anchorButton?.let { anchor ->
                            downloadsPopup?.showDownloadStatus(anchor, fileName, 1)
                        }
                    }
                }.start()
            }
        }, "AndroidDownloader")

        // Standard HTTP / HTTPS downloads via system DownloadManager
        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            if (url.startsWith("data:") || url.startsWith("blob:")) {
                return@DownloadListener
            }

            var guessedFileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
            if (guessedFileName.isNullOrEmpty()) {
                guessedFileName = "download_${System.currentTimeMillis()}"
            }

            try {
                val settings = SettingsPreferences(context)
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimetype)
                    addRequestHeader("User-Agent", userAgent)
                    setDescription(context.getString(R.string.downloader_file_downloading))
                    setTitle(guessedFileName)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    val customPath = settings.downloadPath
                    if (customPath.isNotEmpty() && !customPath.startsWith("content://")) {
                        val targetDir = File(customPath)
                        if (!targetDir.exists()) targetDir.mkdirs()
                        setDestinationUri(Uri.fromFile(File(targetDir, guessedFileName)))
                    } else {
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, guessedFileName)
                    }
                }

                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = downloadManager.enqueue(request)

                anchorButton?.let { anchor ->
                    downloadsPopup?.showDownloadStatus(anchor, guessedFileName, 0)
                }

                val onComplete = object : BroadcastReceiver() {
                    override fun onReceive(ctxt: Context, intent: Intent) {
                        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                        if (id == downloadId) {
                            anchorButton?.let { anchor ->
                                downloadsPopup?.showDownloadStatus(anchor, guessedFileName, 2)
                            }
                            try {
                                ctxt.unregisterReceiver(this)
                            } catch (_: Exception) {}
                        }
                    }
                }

                val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(onComplete, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(onComplete, filter)
                }
            } catch (e: Exception) {
                anchorButton?.let { anchor ->
                    downloadsPopup?.showDownloadStatus(anchor, guessedFileName, 1)
                }
                Toast.makeText(context, "${context.getString(R.string.downloader_file_download_error)}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun downloadFile(
        context: Context,
        url: String,
        suggestedFileName: String? = null,
        downloadsPopup: UIDownloads? = null,
        anchorButton: View? = null
    ) {
        val fileName = suggestedFileName ?: URLUtil.guessFileName(url, null, null) ?: "download_${System.currentTimeMillis()}"
        try {
            val settings = SettingsPreferences(context)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(fileName)
                setDescription(context.getString(R.string.downloader_file_downloading))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                val customPath = settings.downloadPath
                if (customPath.isNotEmpty() && !customPath.startsWith("content://")) {
                    val targetDir = File(customPath)
                    if (!targetDir.exists()) targetDir.mkdirs()
                    setDestinationUri(Uri.fromFile(File(targetDir, fileName)))
                } else {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                }
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            anchorButton?.let { anchor ->
                downloadsPopup?.showDownloadStatus(anchor, fileName, 0)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    const val BLOB_INTERCEPTOR_SCRIPT = """
(function() {
    if (window._downloadInterceptorAdded) return;
    window._downloadInterceptorAdded = true;
    document.addEventListener('click', function(e) {
        var a = e.target.closest('a');
        if (a && a.href && a.href.startsWith('blob:')) {
            e.preventDefault();
            var filename = a.download || ('file_' + Date.now() + '.bin');
            fetch(a.href).then(r => r.blob()).then(blob => {
                const reader = new FileReader();
                reader.onloadend = function() {
                    if (window.AndroidDownloader) {
                        window.AndroidDownloader.downloadBase64File(reader.result, filename);
                    }
                };
                reader.readAsDataURL(blob);
            });
        }
    }, true);
})();
"""
}
