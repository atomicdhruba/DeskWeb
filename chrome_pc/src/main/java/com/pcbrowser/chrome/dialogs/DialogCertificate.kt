package com.pcbrowser.chrome.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.net.http.SslCertificate
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.pcbrowser.chrome.R
import java.text.SimpleDateFormat
import java.util.Locale

class DialogCertificate(
    private val context: Context,
    private val url: String?,
    private val certificate: SslCertificate?
) {

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.app_browser_dialog_certificate, null)
        val ivIcon = view.findViewById<ImageView>(R.id.ivCertStatusIcon)
        val tvStatus = view.findViewById<TextView>(R.id.tvCertStatus)
        val tvDetails = view.findViewById<TextView>(R.id.tvCertDetails)
        val btnClose = view.findViewById<Button>(R.id.btnCloseCert)

        val isHttps = url?.startsWith("https://", ignoreCase = true) == true

        if (certificate != null) {
            ivIcon.setImageResource(R.drawable.ic_lock_24)
            ivIcon.setColorFilter(Color.parseColor("#4caf50"))
            tvStatus.text = "Connection is secure"
            tvStatus.setTextColor(Color.parseColor("#4caf50"))

            val issuedTo = certificate.issuedTo
            val issuedBy = certificate.issuedBy
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val validFrom = dateFormat.format(certificate.validNotBeforeDate)
            val validTo = dateFormat.format(certificate.validNotAfterDate)

            val details = StringBuilder().apply {
                append("URL: ").append(url ?: "").append("\n\n")
                append("--- ISSUED TO ---\n")
                append("Common Name (CN): ").append(issuedTo?.cName ?: "N/A").append("\n")
                append("Organization (O): ").append(issuedTo?.oName ?: "N/A").append("\n")
                append("Org. Unit (OU):   ").append(issuedTo?.uName ?: "N/A").append("\n\n")

                append("--- ISSUED BY ---\n")
                append("Common Name (CN): ").append(issuedBy?.cName ?: "N/A").append("\n")
                append("Organization (O): ").append(issuedBy?.oName ?: "N/A").append("\n")
                append("Org. Unit (OU):   ").append(issuedBy?.uName ?: "N/A").append("\n\n")

                append("--- VALIDITY ---\n")
                append("Valid From: ").append(validFrom).append("\n")
                append("Valid Until: ").append(validTo).append("\n")
            }
            tvDetails.text = details.toString()
        } else if (isHttps) {
            ivIcon.setImageResource(R.drawable.ic_lock_24)
            ivIcon.setColorFilter(Color.parseColor("#4caf50"))
            tvStatus.text = "Connection is secure (HTTPS)"
            tvStatus.setTextColor(Color.parseColor("#4caf50"))
            tvDetails.text = "URL: $url\n\nThe connection to this site is encrypted with standard TLS.\nCertificate details are validated by Android System CA Trust."
        } else {
            ivIcon.setImageResource(R.drawable.ui_browser_ic_info_18)
            ivIcon.setColorFilter(Color.parseColor("#ff5252"))
            tvStatus.text = "Connection is not secure (HTTP)"
            tvStatus.setTextColor(Color.parseColor("#ff5252"))
            tvDetails.text = "URL: $url\n\nYou should not enter any sensitive information (e.g. passwords or credit cards) on this site because it could be seen by attackers."
        }

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
