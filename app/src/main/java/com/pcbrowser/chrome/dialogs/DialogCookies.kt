package com.pcbrowser.chrome.dialogs

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.webkit.CookieManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.pcbrowser.chrome.R

class DialogCookies(
    private val context: Context,
    private val currentUrl: String?,
    private val onCookiesCleared: () -> Unit
) {

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.app_browser_dialog_cookies, null)
        val tvDomain = view.findViewById<TextView>(R.id.tvCookiesDomain)
        val tvContent = view.findViewById<TextView>(R.id.tvCookiesContent)
        val btnClearSite = view.findViewById<Button>(R.id.btnClearSiteCookies)
        val btnClearAll = view.findViewById<Button>(R.id.btnClearAllCookies)
        val btnClose = view.findViewById<Button>(R.id.btnCloseCookies)

        val cookieManager = CookieManager.getInstance()
        val domain = try {
            if (!currentUrl.isNullOrEmpty()) Uri.parse(currentUrl).host else null
        } catch (_: Exception) {
            null
        }

        if (domain != null && !currentUrl.isNullOrEmpty()) {
            tvDomain.text = "Site: $domain"
            val rawCookies = cookieManager.getCookie(currentUrl)
            if (!rawCookies.isNullOrEmpty()) {
                val formatted = rawCookies.split(";").joinToString("\n") { it.trim() }
                tvContent.text = formatted
            } else {
                tvContent.text = "No cookies stored for this site."
            }
        } else {
            tvDomain.text = "No active site loaded."
            tvContent.text = "No active cookies."
            btnClearSite.isEnabled = false
        }

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnClearSite.setOnClickListener {
            if (!currentUrl.isNullOrEmpty()) {
                // Clear cookies for this url
                val rawCookies = cookieManager.getCookie(currentUrl)
                if (!rawCookies.isNullOrEmpty()) {
                    val cookieNames = rawCookies.split(";").map { it.split("=")[0].trim() }
                    for (name in cookieNames) {
                        cookieManager.setCookie(currentUrl, "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT")
                    }
                    cookieManager.flush()
                }
                tvContent.text = "Site cookies cleared."
                Toast.makeText(context, "Cleared cookies for $domain", Toast.LENGTH_SHORT).show()
                onCookiesCleared()
            }
        }

        btnClearAll.setOnClickListener {
            cookieManager.removeAllCookies { success ->
                cookieManager.flush()
                tvContent.text = "All cookies cleared."
                Toast.makeText(context, "All browser cookies cleared", Toast.LENGTH_SHORT).show()
                onCookiesCleared()
            }
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
