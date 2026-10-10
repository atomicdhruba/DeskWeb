package com.pcbrowser.chrome.dialogs

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import com.pcbrowser.chrome.R

class UIDownloads(private val context: Context) : PopupWindow(context) {

    private val popupView: View = LayoutInflater.from(context).inflate(R.layout.app_browser_dialog_download, null, false)
    private val textName: TextView = popupView.findViewById(R.id.text_name)
    private val textTitle: TextView = popupView.findViewById(R.id.text_title)
    private val progressBar: ProgressBar = popupView.findViewById(R.id.progress_bar)
    private val btnOpenDownloads: LinearLayout = popupView.findViewById(R.id.btn_open_downloads)
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        contentView = popupView
        width = LinearLayout.LayoutParams.WRAP_CONTENT
        height = LinearLayout.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        animationStyle = R.style.AnimationDialogMaterial

        popupView.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dismiss()
        }

        btnOpenDownloads.setOnClickListener {
            try {
                val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to generic file manager
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
            dismiss()
        }
    }

    fun showDownloadStatus(anchor: View, fileName: String?, status: Int) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { showDownloadStatus(anchor, fileName, status) }
            return
        }

        val name = fileName ?: context.getString(R.string.browser_dialog_downloads_my_title)
        textName.text = name

        when (status) {
            0 -> { // In progress
                textTitle.text = context.getString(R.string.downloader_file_downloading_wait)
                progressBar.visibility = View.VISIBLE
            }
            1 -> { // Error
                textTitle.text = context.getString(R.string.downloader_file_download_error)
                progressBar.visibility = View.GONE
            }
            2 -> { // Success
                textTitle.text = context.getString(R.string.downloader_file_download_success)
                progressBar.visibility = View.GONE
            }
            3 -> { // Just open popup
                progressBar.visibility = View.GONE
            }
        }

        setBackgroundDrawable(ColorDrawable(0))

        if (isShowing) {
            update()
        } else {
            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val xOffset = -(popupView.measuredWidth - anchor.width)
            showAsDropDown(anchor, xOffset, 0)
        }
    }
}
