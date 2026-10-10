package com.pcbrowser.chrome.tabs

import android.graphics.Bitmap
import android.view.View
import com.pcbrowser.chrome.ui.BrowserTabFragment

data class TabModel(
    val id: String,
    var title: String,
    var url: String,
    var favicon: Bitmap? = null,
    var fragment: BrowserTabFragment? = null,
    var tabView: View? = null,
    var isLoading: Boolean = false
)
