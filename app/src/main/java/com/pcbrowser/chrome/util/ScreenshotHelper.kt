package com.pcbrowser.chrome.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ScreenshotHelper {

    fun captureAndShare(context: Context, webView: WebView) {
        try {
            val width = webView.width
            val height = webView.height
            if (width <= 0 || height <= 0) {
                Toast.makeText(context, "Page is not ready for screenshot", Toast.LENGTH_SHORT).show()
                return
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)

            val screenshotsDir = File(context.cacheDir, "screenshots")
            if (!screenshotsDir.exists()) {
                screenshotsDir.mkdirs()
            }

            val screenshotFile = File(screenshotsDir, "DeskWeb_${System.currentTimeMillis()}.png")
            FileOutputStream(screenshotFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                screenshotFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Page Screenshot")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Page Screenshot"))
        } catch (e: Exception) {
            Toast.makeText(context, "Screenshot failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
