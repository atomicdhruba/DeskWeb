package com.pcbrowser.chrome.dialogs

import android.app.AlertDialog
import android.content.Context
import android.os.Environment
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.pcbrowser.chrome.data.SettingsPreferences
import java.io.File

class DialogDownloadPath(
    private val context: Context,
    private val settings: SettingsPreferences,
    private val onPathChanged: () -> Unit
) {

    fun show() {
        val options = arrayOf(
            "Default (Downloads)",
            "Documents folder",
            "Pictures folder",
            "Enter custom folder path..."
        )

        AlertDialog.Builder(context)
            .setTitle("Download Location")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        settings.downloadPath = ""
                        settings.downloadPathName = "Downloads"
                        Toast.makeText(context, "Downloads set to: Downloads", Toast.LENGTH_SHORT).show()
                        onPathChanged()
                    }
                    1 -> {
                        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                        settings.downloadPath = docs.absolutePath
                        settings.downloadPathName = "Documents"
                        Toast.makeText(context, "Downloads set to: Documents", Toast.LENGTH_SHORT).show()
                        onPathChanged()
                    }
                    2 -> {
                        val pics = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        settings.downloadPath = pics.absolutePath
                        settings.downloadPathName = "Pictures"
                        Toast.makeText(context, "Downloads set to: Pictures", Toast.LENGTH_SHORT).show()
                        onPathChanged()
                    }
                    3 -> showCustomPathInputDialog()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCustomPathInputDialog() {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }

        val hintText = TextView(context).apply {
            text = "Current: " + (if (settings.downloadPath.isNotEmpty()) settings.downloadPath else "Default Downloads")
            textSize = 12f
            setPadding(0, 0, 0, 16)
        }
        container.addView(hintText)

        val input = EditText(context).apply {
            hint = "/storage/emulated/0/Download/DeskWeb"
            setText(settings.downloadPath)
            setSingleLine(true)
        }
        container.addView(input)

        AlertDialog.Builder(context)
            .setTitle("Custom Download Folder")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val path = input.text.toString().trim()
                if (path.isNotEmpty()) {
                    val file = File(path)
                    try {
                        if (!file.exists()) {
                            file.mkdirs()
                        }
                        settings.downloadPath = file.absolutePath
                        settings.downloadPathName = file.name
                        Toast.makeText(context, "Saved download path: ${file.name}", Toast.LENGTH_SHORT).show()
                        onPathChanged()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid path: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
