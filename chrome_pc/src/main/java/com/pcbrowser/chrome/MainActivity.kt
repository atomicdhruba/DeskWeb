package com.pcbrowser.chrome

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.FilterQueryProvider
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.cursoradapter.widget.SimpleCursorAdapter
import com.pcbrowser.chrome.bookmarks.BookmarkManager
import com.pcbrowser.chrome.bookmarks.BookmarkModel
import com.pcbrowser.chrome.data.SettingsPreferences
import com.pcbrowser.chrome.databinding.ActivityMainBinding
import com.pcbrowser.chrome.dialogs.DialogCertificate
import com.pcbrowser.chrome.dialogs.DialogCookies
import com.pcbrowser.chrome.dialogs.DialogDownloadPath
import com.pcbrowser.chrome.dialogs.DialogHistory
import com.pcbrowser.chrome.dialogs.DialogProfile
import com.pcbrowser.chrome.dialogs.DialogSearchEngines
import com.pcbrowser.chrome.dialogs.DialogSettings
import com.pcbrowser.chrome.dialogs.UIDownloads
import com.pcbrowser.chrome.dialogs.UIFinder
import com.pcbrowser.chrome.download.Downloader
import com.pcbrowser.chrome.history.HistoryManager
import com.pcbrowser.chrome.profiles.ProfileManager
import com.pcbrowser.chrome.profiles.ProfileModel
import com.pcbrowser.chrome.provider.MySuggestionProvider
import com.pcbrowser.chrome.search.SearchEngineManager
import com.pcbrowser.chrome.tabs.TabModel
import com.pcbrowser.chrome.tabs.TabsManager
import com.pcbrowser.chrome.util.ScreenshotHelper
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tabsManager: TabsManager
    private lateinit var downloadsPopup: UIDownloads
    private lateinit var findInPage: UIFinder

    private lateinit var settings: SettingsPreferences
    private lateinit var profileManager: ProfileManager
    private lateinit var bookmarkManager: BookmarkManager
    private lateinit var historyManager: HistoryManager

    private var currentZoomPercent: Int = 100
    private var customVideoView: View? = null
    private var isDesktopFullscreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = SettingsPreferences(this)
        profileManager = ProfileManager(this)
        bookmarkManager = BookmarkManager(this)
        historyManager = HistoryManager(this)

        applyImmersiveFullscreen()
        applyOrientation()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyDensity()
        applyNavButtonsVisibility()

        downloadsPopup = UIDownloads(this)
        findInPage = UIFinder(this, binding.overlayContainer) {
            tabsManager.activeTab?.fragment?.webView
        }

        tabsManager = TabsManager(
            activity = this,
            tabStrip = binding.tabStrip,
            tabScrollView = binding.tabScrollView,
            containerId = R.id.tabContainer,
            fragmentManager = supportFragmentManager
        )

        setupOmnibox()
        setupButtons()
        setupProfileButton()
        setupBookmarksBar()
        setupTabStripLayoutListener()
        setupBackPressHandler()

        // Restore tabs for active profile or launch intent
        val intentUrl = intent?.dataString
        val savedTabs = profileManager.getProfileTabs(profileManager.getActiveProfile().id)
        if (!intentUrl.isNullOrEmpty()) {
            tabsManager.addTab(intentUrl, selectNow = true)
        } else if (savedTabs.isNotEmpty()) {
            for (url in savedTabs) {
                tabsManager.addTab(url, selectNow = false)
            }
            if (tabsManager.tabs.isNotEmpty()) {
                tabsManager.selectTab(tabsManager.tabs[0])
            }
        } else {
            tabsManager.addTab("https://www.google.com/webhp", selectNow = true)
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveFullscreen()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveFullscreen()
        }
    }

    override fun onPause() {
        super.onPause()
        val urls = tabsManager.tabs.map { it.url }
        if (urls.isNotEmpty()) {
            profileManager.saveProfileTabs(profileManager.getActiveProfile().id, urls)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { url ->
            openNewTab(url)
        }
    }

    private fun applyImmersiveFullscreen() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isDesktopFullscreen || settings.immersiveMode) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun setDesktopFullscreen(fullscreen: Boolean) {
        isDesktopFullscreen = fullscreen
        if (fullscreen) {
            binding.titleBar.visibility = View.GONE
            binding.toolbar.visibility = View.GONE
            binding.bookmarksBar.visibility = View.GONE
            binding.bookmarksDivider.visibility = View.GONE
            binding.btnExitFullscreen.visibility = View.VISIBLE
            applyImmersiveFullscreen()
            Toast.makeText(this, "Full screen (F11): Press Back or tap Exit", Toast.LENGTH_SHORT).show()
        } else {
            binding.titleBar.visibility = View.VISIBLE
            binding.toolbar.visibility = View.VISIBLE
            binding.btnExitFullscreen.visibility = View.GONE
            refreshBookmarksBar()
            applyImmersiveFullscreen()
        }
    }

    private fun applyOrientation() {
        requestedOrientation = when (settings.orientation) {
            SettingsPreferences.ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            SettingsPreferences.ORIENTATION_AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    private fun applyNavButtonsVisibility() {
        val visibility = if (settings.hideNavButtons) View.GONE else View.VISIBLE
        binding.btnBack.visibility = visibility
        binding.btnFwd.visibility = visibility
        binding.btnReload.visibility = visibility
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun applyDensity() {
        val isCompact = settings.density == SettingsPreferences.DENSITY_COMPACT
        binding.toolbar.layoutParams.height = dpToPx(if (isCompact) 30 else 38)
        binding.tabStrip.layoutParams.height = dpToPx(if (isCompact) 28 else 34)
        binding.titleBar.layoutParams.height = dpToPx(if (isCompact) 30 else 36)
        binding.toolbar.requestLayout()
        binding.tabStrip.requestLayout()
        binding.titleBar.requestLayout()
    }

    fun isAdBlockEnabled(): Boolean = settings.adBlockEnabled
    fun isHttpsOnlyEnabled(): Boolean = settings.httpsOnly
    fun isDoNotTrackEnabled(): Boolean = settings.doNotTrack
    fun recordHistory(url: String, title: String) {
        historyManager.addEntry(profileManager.getActiveProfile().id, url, title)
    }

    fun isDesktopUaEnabled(): Boolean = settings.desktopUserAgent

    private fun setupProfileButton() {
        updateProfileBadge()
        binding.btnProfile.setOnClickListener {
            DialogProfile(
                context = this,
                profileManager = profileManager,
                onProfileSwitched = { newProfile ->
                    switchProfile(newProfile)
                }
            ).show(binding.btnProfile)
        }
    }

    private fun updateProfileBadge() {
        val activeProfile = profileManager.getActiveProfile()
        binding.btnProfile.text = activeProfile.initial
        binding.btnProfile.backgroundTintList = ColorStateList.valueOf(activeProfile.color)
    }

    private fun switchProfile(newProfile: ProfileModel) {
        // 1. Save current open tabs to active profile
        val currentUrls = tabsManager.tabs.map { it.url }
        if (currentUrls.isNotEmpty()) {
            profileManager.saveProfileTabs(profileManager.getActiveProfile().id, currentUrls)
        }

        // 2. Switch active profile
        profileManager.setActiveProfile(newProfile.id)
        updateProfileBadge()

        // 3. Clear tabs & reload for new profile
        tabsManager.clearAllTabs()
        val savedUrls = profileManager.getProfileTabs(newProfile.id)
        if (savedUrls.isNotEmpty()) {
            for (url in savedUrls) {
                tabsManager.addTab(url, selectNow = false)
            }
            if (tabsManager.tabs.isNotEmpty()) {
                tabsManager.selectTab(tabsManager.tabs[0])
            }
        } else {
            tabsManager.addTab("https://www.google.com/webhp", selectNow = true)
        }

        // 4. Reload bookmarks
        refreshBookmarksBar()

        Toast.makeText(this, "Switched to ${newProfile.name}", Toast.LENGTH_SHORT).show()
    }

    private fun getBookmarkIconRes(url: String): Int {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> R.drawable.ic_bm_youtube
            lower.contains("github.com") -> R.drawable.ic_bm_github
            lower.contains("wikipedia.org") -> R.drawable.ic_bm_wikipedia
            lower.contains("reddit.com") -> R.drawable.ic_bm_reddit
            lower.contains("google.com") -> R.drawable.ui_browser_ic_google
            else -> R.drawable.ic_bm_globe
        }
    }

    private fun updateBookmarkStar(url: String) {
        val profileId = profileManager.getActiveProfile().id
        val isBookmarked = bookmarkManager.isBookmarked(profileId, url)
        if (isBookmarked) {
            binding.btnBookmarkStar.setImageResource(R.drawable.ic_bookmark_star_filled_24)
            binding.btnBookmarkStar.contentDescription = "Edit bookmark for this tab"
        } else {
            binding.btnBookmarkStar.setImageResource(R.drawable.ic_bookmark_star_24)
            binding.btnBookmarkStar.contentDescription = "Bookmark this tab"
        }
    }

    private fun showEditBookmarkDialog(bookmark: BookmarkModel) {
        val view = LayoutInflater.from(this).inflate(R.layout.app_browser_dialog_edit_bookmark, null)
        val editName = view.findViewById<EditText>(R.id.editBookmarkName)
        val editUrl = view.findViewById<EditText>(R.id.editBookmarkUrl)

        editName.setText(bookmark.title)
        editUrl.setText(bookmark.url)

        AlertDialog.Builder(this)
            .setTitle("Edit Bookmark")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = editName.text.toString().trim()
                val newUrl = editUrl.text.toString().trim()
                if (newUrl.isNotEmpty()) {
                    val profileId = profileManager.getActiveProfile().id
                    bookmarkManager.updateBookmark(profileId, bookmark.id, newTitle, newUrl)
                    refreshBookmarksBar()
                    tabsManager.activeTab?.let { updateBookmarkStar(it.url) }
                    Toast.makeText(this, "Bookmark updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                val profileId = profileManager.getActiveProfile().id
                bookmarkManager.removeBookmark(profileId, bookmark.id)
                refreshBookmarksBar()
                tabsManager.activeTab?.let { updateBookmarkStar(it.url) }
                Toast.makeText(this, "Bookmark deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupBookmarksBar() {
        refreshBookmarksBar()
        binding.btnAddBookmark.setOnClickListener {
            val activeTab = tabsManager.activeTab
            val url = activeTab?.url ?: ""
            val title = activeTab?.title ?: "Page"
            if (url.isNotEmpty() && url != "about:blank") {
                val profileId = profileManager.getActiveProfile().id
                val existing = bookmarkManager.getBookmarkByUrl(profileId, url)
                if (existing != null) {
                    showEditBookmarkDialog(existing)
                } else {
                    bookmarkManager.addBookmark(profileId, title, url)
                    refreshBookmarksBar()
                    updateBookmarkStar(url)
                    Toast.makeText(this, "Bookmark added: $title", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun refreshBookmarksBar() {
        val isVisible = settings.showBookmarksBar
        binding.bookmarksBar.visibility = if (isVisible) View.VISIBLE else View.GONE
        binding.bookmarksDivider.visibility = if (isVisible) View.VISIBLE else View.GONE
        if (!isVisible) return

        binding.bookmarksContainer.removeAllViews()
        val bookmarks = bookmarkManager.getBookmarks(profileManager.getActiveProfile().id)
        val inflater = LayoutInflater.from(this)

        for (bm in bookmarks) {
            val itemView = inflater.inflate(R.layout.app_browser_bookmark_item, binding.bookmarksContainer, false)
            val tvTitle = itemView.findViewById<TextView>(R.id.bmTitle)
            val ivIcon = itemView.findViewById<ImageView>(R.id.bmIcon)

            tvTitle.text = bm.title
            ivIcon.setImageResource(getBookmarkIconRes(bm.url))

            itemView.setOnClickListener {
                navigateTo(bm.url)
            }

            itemView.setOnLongClickListener {
                val options = arrayOf(
                    "Open in new tab",
                    "Edit bookmark...",
                    "Delete"
                )
                AlertDialog.Builder(this)
                    .setTitle(bm.title)
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> openNewTab(bm.url)
                            1 -> showEditBookmarkDialog(bm)
                            2 -> {
                                val profileId = profileManager.getActiveProfile().id
                                bookmarkManager.removeBookmark(profileId, bm.id)
                                refreshBookmarksBar()
                                tabsManager.activeTab?.let { updateBookmarkStar(it.url) }
                                Toast.makeText(this, "Bookmark deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .show()
                true
            }

            binding.bookmarksContainer.addView(itemView)
        }
    }

    private fun setupOmnibox() {
        val adapter = SimpleCursorAdapter(
            this,
            R.layout.app_browser_suggest,
            null,
            arrayOf("suggest_text_1"),
            intArrayOf(android.R.id.text1),
            0
        )

        adapter.filterQueryProvider = FilterQueryProvider { constraint ->
            contentResolver.query(
                MySuggestionProvider.URI_SEARCH,
                null,
                null,
                arrayOf(constraint?.toString() ?: ""),
                null
            )
        }

        adapter.cursorToStringConverter = SimpleCursorAdapter.CursorToStringConverter { cursor ->
            cursor.getString(cursor.getColumnIndexOrThrow("suggest_text_1"))
        }

        binding.addressBar.setAdapter(adapter)
        binding.addressBar.setDropDownBackgroundResource(R.drawable.ui_browser_suggest_bg)

        binding.addressBar.setOnItemClickListener { parent, _, position, _ ->
            val cursor = parent.getItemAtPosition(position) as? Cursor
            val text = cursor?.getString(cursor.getColumnIndexOrThrow("suggest_text_1"))
            if (!text.isNullOrEmpty()) {
                navigateTo(text)
                hideKeyboard()
            }
        }

        binding.addressBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val input = binding.addressBar.text.toString().trim()
                if (input.isNotEmpty()) {
                    navigateTo(input)
                    hideKeyboard()
                }
                return@setOnEditorActionListener true
            }
            false
        }

        binding.addressBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                binding.btnClearAddress.visibility = if (s.isNullOrEmpty() || !binding.addressBar.hasFocus()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.addressBar.setOnFocusChangeListener { _, hasFocus ->
            binding.btnClearAddress.visibility = if (hasFocus && binding.addressBar.text.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        binding.addressBar.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableStart = binding.addressBar.compoundDrawables[0]
                if (drawableStart != null && event.x <= binding.addressBar.paddingStart + drawableStart.bounds.width() + 24) {
                    val frag = tabsManager.activeTab?.fragment
                    DialogCertificate(this, tabsManager.activeTab?.url, frag?.currentCertificate).show()
                    return@setOnTouchListener true
                }
            }
            false
        }

        binding.btnClearAddress.setOnClickListener {
            binding.addressBar.setText("")
        }

        binding.btnBookmarkStar.setOnClickListener {
            val active = tabsManager.activeTab ?: return@setOnClickListener
            val url = active.url
            val title = active.title.ifBlank { "Page" }
            if (url.isBlank() || url == "about:blank") return@setOnClickListener

            val profileId = profileManager.getActiveProfile().id
            val existing = bookmarkManager.getBookmarkByUrl(profileId, url)
            if (existing != null) {
                showEditBookmarkDialog(existing)
            } else {
                bookmarkManager.addBookmark(profileId, title, url)
                refreshBookmarksBar()
                updateBookmarkStar(url)
                Toast.makeText(this, "Bookmark added: $title", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupButtons() {
        binding.btnNewTab.setOnClickListener {
            openNewTab()
        }

        binding.btnBack.setOnClickListener {
            tabsManager.activeTab?.fragment?.goBack()
        }

        binding.btnFwd.setOnClickListener {
            tabsManager.activeTab?.fragment?.goForward()
        }

        binding.btnReload.setOnClickListener {
            val active = tabsManager.activeTab ?: return@setOnClickListener
            if (active.isLoading) {
                active.fragment?.stopLoading()
            } else {
                active.fragment?.reload()
            }
        }

        binding.btnDownloads.setOnClickListener {
            downloadsPopup.showDownloadStatus(binding.btnDownloads, null, 3)
        }

        binding.btnMenu.setOnClickListener {
            showSettingsMenu()
        }

        binding.btnExitFullscreen.setOnClickListener {
            setDesktopFullscreen(false)
        }
    }

    private fun setupTabStripLayoutListener() {
        binding.titleBar.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            val available = (right - left) - binding.btnNewTab.width - 24
            binding.tabScrollView.setMaxWidth(available)
            tabsManager.recalculateTabWidths()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isDesktopFullscreen) {
                    setDesktopFullscreen(false)
                    return
                }
                if (customVideoView != null) {
                    hideFullscreenVideo(customVideoView)
                    return
                }
                if (findInPage.isVisible()) {
                    findInPage.hide()
                    return
                }
                val activeTab = tabsManager.activeTab
                if (activeTab?.fragment?.canGoBack() == true) {
                    activeTab.fragment?.goBack()
                    return
                }
                if (tabsManager.tabs.size > 1 && activeTab != null) {
                    tabsManager.closeTab(activeTab)
                    return
                }
                finish()
            }
        })
    }

    fun navigateTo(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        // Smart Detection for Phone Numbers
        if (Patterns.PHONE.matcher(trimmed).matches() && trimmed.length >= 7 && !trimmed.contains(".") && !trimmed.contains("/")) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.omnibox_open_as))
                .setItems(arrayOf(getString(R.string.omnibox_call, trimmed), "Search for \"$trimmed\"")) { _, which ->
                    if (which == 0) {
                        try {
                            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$trimmed")))
                        } catch (e: Exception) {
                            Toast.makeText(this, "Cannot dial: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        performNavigation(SearchEngineManager.buildSearchUrl(settings.activeSearchEngineId, trimmed), trimmed)
                    }
                }
                .show()
            binding.addressBar.clearFocus()
            return
        }

        // Smart Detection for Email addresses
        if (Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.omnibox_open_as))
                .setItems(arrayOf(getString(R.string.omnibox_email, trimmed), "Search for \"$trimmed\"")) { _, which ->
                    if (which == 0) {
                        try {
                            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$trimmed")))
                        } catch (e: Exception) {
                            Toast.makeText(this, "Cannot email: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        performNavigation(SearchEngineManager.buildSearchUrl(settings.activeSearchEngineId, trimmed), trimmed)
                    }
                }
                .show()
            binding.addressBar.clearFocus()
            return
        }

        val targetUrl = if (Patterns.WEB_URL.matcher(trimmed).matches() || trimmed.startsWith("about:") || trimmed.startsWith("chrome:")) {
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("about:") || trimmed.startsWith("chrome:")) {
                trimmed
            } else {
                "https://$trimmed"
            }
        } else {
            SearchEngineManager.buildSearchUrl(settings.activeSearchEngineId, trimmed)
        }

        performNavigation(targetUrl, trimmed)
    }

    private fun performNavigation(targetUrl: String, term: String) {
        try {
            val values = ContentValues().apply {
                put("term", term)
                put("last_used", System.currentTimeMillis())
            }
            contentResolver.insert(MySuggestionProvider.URI_SUGGESTIONS, values)
        } catch (_: Exception) {}

        tabsManager.activeTab?.fragment?.loadUrl(targetUrl)
        binding.addressBar.clearFocus()
    }

    fun openNewTab(url: String = SearchEngineManager.getEngine(settings.activeSearchEngineId).homepageUrl) {
        tabsManager.addTab(url, selectNow = true)
    }

    fun saveImage(url: String) {
        Downloader.downloadFile(this, url, null, downloadsPopup, binding.btnDownloads)
    }

    fun getDownloadsPopup(): UIDownloads = downloadsPopup

    fun getDownloadsButton(): ImageButton = binding.btnDownloads

    fun onActiveTabChanged(tab: TabModel) {
        binding.addressBar.setAddress(tab.url)
        updateNavButtons(tab)
        updateBookmarkStar(tab.url)
    }

    fun onActiveTabLoadingChanged(tab: TabModel) {
        if (tab == tabsManager.activeTab) {
            updateNavButtons(tab)
        }
    }

    fun onTabProgress(tabId: String, progress: Int) {
        tabsManager.updateProgress(tabId, progress)
        if (tabsManager.activeTab?.id == tabId) {
            binding.pageProgressBar.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
            binding.pageProgressBar.progress = progress
        }
    }

    fun onTabTitle(tabId: String, title: String) {
        tabsManager.updateTitle(tabId, title)
    }

    fun onTabFavicon(tabId: String, icon: Bitmap?) {
        tabsManager.updateFavicon(tabId, icon)
    }

    fun onTabUrl(tabId: String, url: String, isLoading: Boolean) {
        tabsManager.updateUrl(tabId, url, isLoading)
        if (tabsManager.activeTab?.id == tabId) {
            binding.addressBar.setAddress(url)
            val lockIcon = when {
                url.startsWith("https://", ignoreCase = true) -> R.drawable.ic_lock_24
                url.startsWith("http://", ignoreCase = true) -> R.drawable.ui_browser_ic_info_18
                else -> R.drawable.ui_browser_ic_google
            }
            binding.addressBar.setCompoundDrawablesWithIntrinsicBounds(lockIcon, 0, 0, 0)
            val tab = tabsManager.activeTab ?: return
            updateNavButtons(tab)
            updateBookmarkStar(url)
        }
    }

    private fun updateNavButtons(tab: TabModel) {
        val frag = tab.fragment
        binding.btnBack.isEnabled = frag?.canGoBack() ?: false
        binding.btnFwd.isEnabled = frag?.canGoForward() ?: false

        if (tab.isLoading) {
            binding.btnReload.setImageResource(R.drawable.app_title_ic_close_16)
            binding.btnReload.contentDescription = getString(R.string.nav_stop)
        } else {
            binding.btnReload.setImageResource(R.drawable.ui_browser_ic_reload_20)
            binding.btnReload.contentDescription = getString(R.string.nav_reload)
        }
    }

    fun showFullscreenVideo(view: View?) {
        if (view == null) return
        customVideoView = view
        binding.tabContainer.visibility = View.GONE
        binding.titleBar.visibility = View.GONE
        binding.toolbar.visibility = View.GONE
        binding.bookmarksBar.visibility = View.GONE
        binding.bookmarksDivider.visibility = View.GONE

        binding.fullscreenCustomViewContainer.apply {
            removeAllViews()
            addView(view)
            visibility = View.VISIBLE
        }

        applyImmersiveFullscreen()
    }

    fun hideFullscreenVideo(view: View?) {
        customVideoView = null
        binding.fullscreenCustomViewContainer.apply {
            removeAllViews()
            visibility = View.GONE
        }
        binding.tabContainer.visibility = View.VISIBLE
        binding.titleBar.visibility = View.VISIBLE
        binding.toolbar.visibility = View.VISIBLE
        if (settings.showBookmarksBar) {
            binding.bookmarksBar.visibility = View.VISIBLE
            binding.bookmarksDivider.visibility = View.VISIBLE
        }

        applyImmersiveFullscreen()
    }

    fun handleShortcut(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_F11 -> {
                setDesktopFullscreen(!isDesktopFullscreen)
                return true
            }
            KeyEvent.KEYCODE_ESCAPE -> {
                if (isDesktopFullscreen) {
                    setDesktopFullscreen(false)
                    return true
                }
            }
            KeyEvent.KEYCODE_T -> {
                openNewTab()
                return true
            }
            KeyEvent.KEYCODE_W -> {
                tabsManager.activeTab?.let { tabsManager.closeTab(it) }
                return true
            }
            KeyEvent.KEYCODE_R -> {
                tabsManager.activeTab?.fragment?.reload()
                return true
            }
            KeyEvent.KEYCODE_F -> {
                findInPage.show()
                return true
            }
            KeyEvent.KEYCODE_P -> {
                printCurrentPage()
                return true
            }
            KeyEvent.KEYCODE_L -> {
                binding.addressBar.requestFocus()
                showKeyboard()
                return true
            }
        }
        return false
    }

    private fun showSettingsMenu() {
        val dialog = DialogSettings(this, settings, currentZoomPercent) { action, zoomPercent ->
            when (action) {
                DialogSettings.Action.NEW_TAB -> openNewTab()
                DialogSettings.Action.HISTORY -> {
                    DialogHistory(this, historyManager, profileManager.getActiveProfile().id) { url ->
                        navigateTo(url)
                    }.show()
                }
                DialogSettings.Action.FULLSCREEN -> setDesktopFullscreen(!isDesktopFullscreen)
                DialogSettings.Action.ZOOM_CHANGE -> {
                    currentZoomPercent = zoomPercent
                    tabsManager.activeTab?.fragment?.setZoom(zoomPercent)
                }
                DialogSettings.Action.TOGGLE_DENSITY -> {
                    settings.density = if (settings.density == SettingsPreferences.DENSITY_COMPACT) {
                        SettingsPreferences.DENSITY_COMFORTABLE
                    } else {
                        SettingsPreferences.DENSITY_COMPACT
                    }
                    applyDensity()
                    val label = if (settings.density == SettingsPreferences.DENSITY_COMPACT) "Compact" else "Comfortable"
                    Toast.makeText(this, "UI Density: $label", Toast.LENGTH_SHORT).show()
                }
                DialogSettings.Action.ORIENTATION_PICKER -> showOrientationDialog()
                DialogSettings.Action.TOGGLE_NAV_BUTTONS -> {
                    settings.hideNavButtons = !settings.hideNavButtons
                    applyNavButtonsVisibility()
                    val state = if (settings.hideNavButtons) "Hidden (Desktop Mode)" else "Visible"
                    Toast.makeText(this, "Navigation Buttons: $state", Toast.LENGTH_SHORT).show()
                }
                DialogSettings.Action.MANAGE_PROFILES -> {
                    DialogProfile(
                        context = this,
                        profileManager = profileManager,
                        onProfileSwitched = { switchProfile(it) }
                    ).show(binding.btnMenu)
                }
                DialogSettings.Action.TOGGLE_BOOKMARKS_BAR -> {
                    settings.showBookmarksBar = !settings.showBookmarksBar
                    refreshBookmarksBar()
                    val state = if (settings.showBookmarksBar) "Shown" else "Hidden"
                    Toast.makeText(this, "Bookmarks Bar: $state", Toast.LENGTH_SHORT).show()
                }
                DialogSettings.Action.TOGGLE_IMMERSIVE -> {
                    settings.immersiveMode = !settings.immersiveMode
                    applyImmersiveFullscreen()
                    val state = if (settings.immersiveMode) "Hidden (Immersive)" else "Visible"
                    Toast.makeText(this, "System Bars: $state", Toast.LENGTH_SHORT).show()
                }
                DialogSettings.Action.TOGGLE_DESKTOP_UA -> {
                    settings.desktopUserAgent = !settings.desktopUserAgent
                    val isDesktop = settings.desktopUserAgent
                    tabsManager.activeTab?.fragment?.updateDesktopMode(isDesktop)
                    val state = if (isDesktop) "Desktop Site" else "Mobile Site"
                    Toast.makeText(this, "User-Agent: $state", Toast.LENGTH_SHORT).show()
                }
                DialogSettings.Action.TOGGLE_AD_BLOCK -> {
                    settings.adBlockEnabled = !settings.adBlockEnabled
                    val state = if (settings.adBlockEnabled) "Enabled" else "Disabled"
                    Toast.makeText(this, "Ad Blocker: $state", Toast.LENGTH_SHORT).show()
                    tabsManager.activeTab?.fragment?.reload()
                }
                DialogSettings.Action.TOGGLE_HTTPS_ONLY -> {
                    settings.httpsOnly = !settings.httpsOnly
                    val state = if (settings.httpsOnly) "Enabled" else "Disabled"
                    Toast.makeText(this, "HTTPS-Only Mode: $state", Toast.LENGTH_SHORT).show()
                }
                DialogSettings.Action.TOGGLE_DNT -> {
                    settings.doNotTrack = !settings.doNotTrack
                    val state = if (settings.doNotTrack) "Enabled" else "Disabled"
                    Toast.makeText(this, "Do Not Track: $state", Toast.LENGTH_SHORT).show()
                }
                DialogSettings.Action.MANAGE_COOKIES -> {
                    val activeUrl = tabsManager.activeTab?.url
                    DialogCookies(this, activeUrl) {
                        tabsManager.activeTab?.fragment?.reload()
                    }.show()
                }
                DialogSettings.Action.CERT_INFO -> {
                    val frag = tabsManager.activeTab?.fragment
                    val activeUrl = tabsManager.activeTab?.url
                    DialogCertificate(this, activeUrl, frag?.currentCertificate).show()
                }
                DialogSettings.Action.SEARCH_ENGINES -> {
                    DialogSearchEngines(this, settings) { engineId ->
                        val engine = SearchEngineManager.getEngine(engineId)
                        Toast.makeText(this, "Default search engine: ${engine.name}", Toast.LENGTH_SHORT).show()
                    }.show()
                }
                DialogSettings.Action.DOWNLOAD_PATH -> {
                    DialogDownloadPath(this, settings) {
                        Toast.makeText(this, "Download location updated", Toast.LENGTH_SHORT).show()
                    }.show()
                }
                DialogSettings.Action.SCREENSHOT -> {
                    val webView = tabsManager.activeTab?.fragment?.webView
                    if (webView != null) {
                        ScreenshotHelper.captureAndShare(this, webView)
                    } else {
                        Toast.makeText(this, "No page to capture", Toast.LENGTH_SHORT).show()
                    }
                }
                DialogSettings.Action.PRINT -> printCurrentPage()
                DialogSettings.Action.TRANSLATE -> {
                    val activeUrl = tabsManager.activeTab?.url
                    if (!activeUrl.isNullOrEmpty()) {
                        val translateUrl = "https://translate.google.com/translate?u=" + URLEncoder.encode(activeUrl, "UTF-8")
                        openNewTab(translateUrl)
                    }
                }
                DialogSettings.Action.FIND_IN_PAGE -> findInPage.show()
                DialogSettings.Action.ABOUT -> showAboutDialog()
                DialogSettings.Action.EXIT -> finish()
            }
        }

        dialog.show(binding.btnMenu)
    }

    private fun showOrientationDialog() {
        val options: Array<CharSequence> = arrayOf(
            getString(R.string.orientation_landscape),
            getString(R.string.orientation_portrait),
            getString(R.string.orientation_auto)
        )
        val currentIndex = when (settings.orientation) {
            SettingsPreferences.ORIENTATION_PORTRAIT -> 1
            SettingsPreferences.ORIENTATION_AUTO -> 2
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.browser_menu_orientation))
            .setSingleChoiceItems(options, currentIndex) { dialog: DialogInterface, which: Int ->
                when (which) {
                    0 -> settings.orientation = SettingsPreferences.ORIENTATION_LANDSCAPE
                    1 -> settings.orientation = SettingsPreferences.ORIENTATION_PORTRAIT
                    2 -> settings.orientation = SettingsPreferences.ORIENTATION_AUTO
                }
                applyOrientation()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun printCurrentPage() {
        val webView = tabsManager.activeTab?.fragment?.webView ?: return
        val printManager = getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = webView.createPrintDocumentAdapter("Chrome PC Document")
        printManager.print("Chrome PC Print", adapter, PrintAttributes.Builder().build())
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage("Chrome PC - Standalone Desktop Web Browser for Android\nReplicated from HyperDroid desktop browser subsystem.")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.addressBar.windowToken, 0)
    }

    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.addressBar, InputMethodManager.SHOW_IMPLICIT)
    }
}
