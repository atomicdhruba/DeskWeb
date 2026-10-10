package com.pcbrowser.chrome.tabs

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.pcbrowser.chrome.MainActivity
import com.pcbrowser.chrome.R
import com.pcbrowser.chrome.ui.BrowserTabFragment
import com.pcbrowser.chrome.ui.widgets.MaxWidthHorizontalScrollView

class TabsManager(
    private val activity: MainActivity,
    private val tabStrip: LinearLayout,
    private val tabScrollView: MaxWidthHorizontalScrollView,
    private val containerId: Int,
    private val fragmentManager: FragmentManager
) {

    val tabs = ArrayList<TabModel>()
    var activeTab: TabModel? = null
        private set

    private var tabCounter = 0

    fun addTab(url: String, selectNow: Boolean = true): TabModel {
        val tabId = "tab_${++tabCounter}"
        val tabModel = TabModel(
            id = tabId,
            title = activity.getString(R.string.browser_tab_new),
            url = url
        )

        val tabView = LayoutInflater.from(activity).inflate(
            R.layout.app_browser_tab_item,
            tabStrip,
            false
        )

        val tabTitle = tabView.findViewById<TextView>(R.id.tabTitle)
        val btnClose = tabView.findViewById<ImageButton>(R.id.btn_close_tab)
        val spinner = tabView.findViewById<ProgressBar>(R.id.spinner)

        tabTitle.text = tabModel.title
        tabModel.tabView = tabView

        tabView.setOnClickListener {
            selectTab(tabModel)
        }

        btnClose.setOnClickListener {
            closeTab(tabModel)
        }

        val fragment = BrowserTabFragment.newInstance(tabId, url)
        tabModel.fragment = fragment

        tabs.add(tabModel)
        tabStrip.addView(tabView)

        recalculateTabWidths()

        if (selectNow) {
            selectTab(tabModel)
        }

        tabScrollView.post {
            tabScrollView.smoothScrollTo(tabView.right, 0)
        }

        return tabModel
    }

    fun selectTab(targetTab: TabModel) {
        if (targetTab == activeTab && targetTab.fragment?.isAdded == true) {
            return
        }

        val transaction = fragmentManager.beginTransaction()

        activeTab?.let { current ->
            current.tabView?.isSelected = false
            current.fragment?.let { frag ->
                if (frag.isAdded) {
                    transaction.hide(frag)
                }
            }
        }

        val targetFragment = targetTab.fragment
        if (targetFragment != null) {
            if (targetFragment.isAdded) {
                transaction.show(targetFragment)
            } else {
                transaction.add(containerId, targetFragment, targetTab.id)
            }
        }

        transaction.commitAllowingStateLoss()

        targetTab.tabView?.isSelected = true
        activeTab = targetTab

        activity.onActiveTabChanged(targetTab)
    }

    fun closeTab(tabToClose: TabModel) {
        val index = tabs.indexOf(tabToClose)
        if (index < 0) return

        val transaction = fragmentManager.beginTransaction()
        tabToClose.fragment?.let { frag ->
            if (frag.isAdded) {
                transaction.remove(frag)
            }
        }
        transaction.commitAllowingStateLoss()

        tabToClose.tabView?.let { tabStrip.removeView(it) }
        tabs.remove(tabToClose)

        recalculateTabWidths()

        if (tabs.isEmpty()) {
            activity.finish()
            return
        }

        if (tabToClose == activeTab) {
            val nextIndex = if (index >= tabs.size) tabs.size - 1 else index
            selectTab(tabs[nextIndex])
        }
    }

    fun clearAllTabs() {
        val transaction = fragmentManager.beginTransaction()
        for (tab in tabs) {
            tab.fragment?.let { frag ->
                if (frag.isAdded) {
                    transaction.remove(frag)
                }
            }
            tab.tabView?.let { tabStrip.removeView(it) }
        }
        transaction.commitAllowingStateLoss()
        tabs.clear()
        activeTab = null
        tabCounter = 0
    }

    fun updateTitle(tabId: String, title: String) {
        val tab = tabs.find { it.id == tabId } ?: return
        tab.title = title
        tab.tabView?.findViewById<TextView>(R.id.tabTitle)?.text = title
    }

    fun updateFavicon(tabId: String, favicon: Bitmap?) {
        val tab = tabs.find { it.id == tabId } ?: return
        tab.favicon = favicon
        val tabTitle = tab.tabView?.findViewById<TextView>(R.id.tabTitle) ?: return

        if (favicon != null) {
            val density = activity.resources.displayMetrics.density
            val size = (16 * density).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(favicon, size, size, true)
            val drawable = BitmapDrawable(activity.resources, scaledBitmap)
            drawable.setBounds(0, 0, size, size)
            tabTitle.setCompoundDrawablesRelative(drawable, null, null, null)
        } else {
            tabTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ui_browser_icon_tint, 0, 0, 0
            )
        }
    }

    fun updateProgress(tabId: String, progress: Int) {
        val tab = tabs.find { it.id == tabId } ?: return
        val spinner = tab.tabView?.findViewById<ProgressBar>(R.id.spinner)
        spinner?.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
    }

    fun updateUrl(tabId: String, url: String, isLoading: Boolean) {
        val tab = tabs.find { it.id == tabId } ?: return
        tab.url = url
        tab.isLoading = isLoading
        if (tab == activeTab) {
            activity.onActiveTabLoadingChanged(tab)
        }
    }

    fun recalculateTabWidths() {
        val childCount = tabStrip.childCount
        if (childCount == 0) return

        val availableWidth = if (tabScrollView.getMaxWidth() > 0) {
            tabScrollView.getMaxWidth()
        } else {
            val w = tabScrollView.width
            if (w > 0) w else {
                val dm = activity.resources.displayMetrics
                dm.widthPixels - (childCount * 4)
            }
        }

        val minWidth = activity.resources.getDimensionPixelSize(R.dimen.ui_84dp)
        val maxWidth = activity.resources.getDimensionPixelSize(R.dimen.ui_156dp)
        val margin = activity.resources.getDimensionPixelSize(R.dimen.ui_4dp)

        var targetWidth = maxWidth
        val totalMaxWidth = targetWidth * childCount
        val remainingSpace = availableWidth - (margin * childCount)

        if (totalMaxWidth > remainingSpace && childCount > 0) {
            targetWidth = remainingSpace / childCount
            if (targetWidth < minWidth) {
                targetWidth = minWidth
            }
        }

        for (i in 0 until childCount) {
            val child = tabStrip.getChildAt(i)
            val params = child.layoutParams as? LinearLayout.LayoutParams
                ?: LinearLayout.LayoutParams(targetWidth, LinearLayout.LayoutParams.MATCH_PARENT)
            params.width = targetWidth
            child.layoutParams = params
        }

        val needScroll = (targetWidth == minWidth && minWidth * childCount > availableWidth)
        tabScrollView.isFillViewport = needScroll
    }
}
