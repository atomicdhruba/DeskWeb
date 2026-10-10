package com.pcbrowser.chrome.engine

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

object AdBlocker {

    private val blockedHosts = ConcurrentHashMap.newKeySet<String>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        Thread {
            try {
                context.assets.open("adblock_hosts.txt").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            val trimmed = line.trim().lowercase()
                            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                                blockedHosts.add(trimmed)
                            }
                            line = reader.readLine()
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback basic ad domains if assets read fails
                blockedHosts.addAll(
                    listOf(
                        "doubleclick.net",
                        "googleadservices.com",
                        "googlesyndication.com",
                        "adservice.google.com",
                        "adnxs.com",
                        "criteo.com",
                        "taboola.com",
                        "outbrain.com"
                    )
                )
            }
        }.start()
    }

    fun isAd(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val host = try {
            Uri.parse(url).host?.lowercase() ?: return false
        } catch (_: Exception) {
            return false
        }

        if (blockedHosts.contains(host)) return true

        // Check subdomain matching (e.g. ad.example.com vs example.com)
        for (blocked in blockedHosts) {
            if (host.endsWith(".$blocked")) {
                return true
            }
        }
        return false
    }

    fun createEmptyResource(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
