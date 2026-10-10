package com.pcbrowser.chrome.history

data class HistoryModel(
    val id: Long,
    val url: String,
    val title: String,
    val timestamp: Long
)
