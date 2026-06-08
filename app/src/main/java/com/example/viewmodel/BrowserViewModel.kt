package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.theme.BrowserThemeId
import com.example.api.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class TabState(
    val id: String,
    val title: String,
    val currentUrl: String,
    val isIncognito: Boolean,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val loadingProgress: Int = 0,
    val isReaderMode: Boolean = false,
    val readerTitle: String = "",
    val readerParagraphs: List<String> = emptyList()
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BrowserDatabase.getDatabase(application)
    private val repository = BrowserRepository(db.browserDao())
    private val prefs = application.getSharedPreferences("apex_browser_settings", Context.MODE_PRIVATE)

    // --- Customization Settings state ---
    private val _theme = MutableStateFlow(BrowserThemeId.BOLD_TYPOGRAPHY)
    val theme: StateFlow<BrowserThemeId> = _theme.asStateFlow()

    private val _addressBarBottom = MutableStateFlow(false)
    val addressBarBottom: StateFlow<Boolean> = _addressBarBottom.asStateFlow()

    private val _searchEngine = MutableStateFlow("google") // google, duckduckgo, bing, ecosia
    val searchEngine: StateFlow<String> = _searchEngine.asStateFlow()

    private val _adBlockerEnabled = MutableStateFlow(true)
    val adBlockerEnabled: StateFlow<Boolean> = _adBlockerEnabled.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f) // 0.8f, 1.0f, 1.2f, 1.4f
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _desktopMode = MutableStateFlow(false)
    val desktopMode: StateFlow<Boolean> = _desktopMode.asStateFlow()

    private val _javascriptEnabled = MutableStateFlow(true)
    val javascriptEnabled: StateFlow<Boolean> = _javascriptEnabled.asStateFlow()

    // --- Bookmarks & History flows from Room ---
    val bookmarks = repository.allBookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val history = repository.allHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Tab Management State ---
    private val _tabs = MutableStateFlow<List<TabState>>(emptyList())
    val tabs: StateFlow<List<TabState>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    val activeTab: StateFlow<TabState?> = combine(_tabs, _activeTabId) { tabsList, activeId ->
        tabsList.find { it.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Gemini AI Sidekick Companion State ---
    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    // --- UI state helper fields ---
    val isIncognito: StateFlow<Boolean> = activeTab.map { it?.isIncognito == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Load persistable configurations
        loadSettings()
        // Restore tab sessions from DB
        viewModelScope.launch {
            repository.allSavedTabs.first().let { savedTabs ->
                if (savedTabs.isNotEmpty()) {
                    val list = savedTabs.map {
                        TabState(
                            id = it.id,
                            title = it.title,
                            currentUrl = it.url,
                            isIncognito = it.isIncognito
                        )
                    }
                    _tabs.value = list
                    _activeTabId.value = list.firstOrNull()?.id ?: ""
                } else {
                    // Create basic default tab if database was empty
                    createNewTab("https://www.google.com")
                }
            }
        }
    }

    // --- Settings management ---
    private fun loadSettings() {
        val themeName = prefs.getString("theme", BrowserThemeId.BOLD_TYPOGRAPHY.name) ?: BrowserThemeId.BOLD_TYPOGRAPHY.name
        _theme.value = try { BrowserThemeId.valueOf(themeName) } catch(e: Exception) { BrowserThemeId.BOLD_TYPOGRAPHY }
        _addressBarBottom.value = prefs.getBoolean("address_bar_bottom", false)
        _searchEngine.value = prefs.getString("search_engine", "google") ?: "google"
        _adBlockerEnabled.value = prefs.getBoolean("ad_blocker_enabled", true)
        _fontScale.value = prefs.getFloat("font_scale", 1.0f)
        _desktopMode.value = prefs.getBoolean("desktop_mode", false)
        _javascriptEnabled.value = prefs.getBoolean("js_enabled", true)
    }

    fun updateTheme(newTheme: BrowserThemeId) {
        _theme.value = newTheme
        prefs.edit().putString("theme", newTheme.name).apply()
    }

    fun toggleAddressBarLayout() {
        val newValue = !_addressBarBottom.value
        _addressBarBottom.value = newValue
        prefs.edit().putBoolean("address_bar_bottom", newValue).apply()
    }

    fun updateSearchEngine(engine: String) {
        _searchEngine.value = engine
        prefs.edit().putString("search_engine", engine).apply()
    }

    fun toggleAdBlocker() {
        val newValue = !_adBlockerEnabled.value
        _adBlockerEnabled.value = newValue
        prefs.edit().putBoolean("ad_blocker_enabled", newValue).apply()
    }

    fun updateFontScale(scale: Float) {
        _fontScale.value = scale
        prefs.edit().putFloat("font_scale", scale).apply()
    }

    fun toggleDesktopMode() {
        val newValue = !_desktopMode.value
        _desktopMode.value = newValue
        prefs.edit().putBoolean("desktop_mode", newValue).apply()
    }

    fun toggleJavaScript() {
        val newValue = !_javascriptEnabled.value
        _javascriptEnabled.value = newValue
        prefs.edit().putBoolean("js_enabled", newValue).apply()
    }

    // --- Tab actions ---
    fun createNewTab(url: String = "homepage", isIncognito: Boolean = false) {
        val targetUrl = if (url == "homepage") "homepage" else sanitizeUrl(url)
        val tabId = UUID.randomUUID().toString()
        val newTab = TabState(
            id = tabId,
            title = if (targetUrl == "homepage") "New Tab" else "Loading...",
            currentUrl = targetUrl,
            isIncognito = isIncognito
        )

        _tabs.value = _tabs.value + newTab
        _activeTabId.value = tabId

        // Save to DB (only preserve non-incognito tabs for launch persistence)
        if (!isIncognito && targetUrl != "homepage") {
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveTab(SavedTab(id = tabId, title = newTab.title, url = targetUrl, isIncognito = false))
            }
        }
    }

    fun closeTab(tabId: String) {
        val tabToClose = _tabs.value.find { it.id == tabId } ?: return
        val newTabsList = _tabs.value.filter { it.id != tabId }

        if (newTabsList.isEmpty()) {
            _tabs.value = emptyList()
            createNewTab("homepage", false)
        } else {
            _tabs.value = newTabsList
            if (_activeTabId.value == tabId) {
                _activeTabId.value = newTabsList.last().id
            }
        }

        // Delete from Room DB
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTabById(tabId)
        }
    }

    fun switchActiveTab(tabId: String) {
        _activeTabId.value = tabId
        // Update DB sorting weight or last active
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy() else it
        }
    }

    fun updateTabUrlState(tabId: String, url: String, title: String, canGoBack: Boolean, canGoForward: Boolean) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                val updated = tab.copy(
                    currentUrl = url,
                    title = title.ifEmpty { url },
                    canGoBack = canGoBack,
                    canGoForward = canGoForward
                )
                // Persistence update
                if (!tab.isIncognito && url != "homepage") {
                    viewModelScope.launch(Dispatchers.IO) {
                        repository.saveTab(SavedTab(id = tabId, title = updated.title, url = url, isIncognito = false))
                    }
                }
                updated
            } else tab
        }
    }

    fun updateTabLoadingProgress(tabId: String, progress: Int) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) tab.copy(loadingProgress = progress) else tab
        }
    }

    // --- Bookmarks & History modifiers ---
    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isCurrentlyBookmarked = bookmarks.value.any { it.url == url }
            if (isCurrentlyBookmarked) {
                repository.removeBookmarkByUrl(url)
            } else {
                repository.addBookmark(Bookmark(title = title, url = url))
            }
        }
    }

    fun addHistory(title: String, url: String, isIncognito: Boolean) {
        if (isIncognito || url == "homepage" || url.isBlank() || url.startsWith("javascript:")) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addHistoryItem(HistoryItem(title = title, url = url))
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHistoryById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    fun clearSavedTabs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearSavedTabs()
            // Reset tab engine back to a single fresh tab
            _tabs.value = emptyList()
            createNewTab("homepage", false)
        }
    }

    // --- Reader Mode ---
    fun setReaderMode(tabId: String, enabled: Boolean, title: String = "", paragraphs: List<String> = emptyList()) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(
                    isReaderMode = enabled,
                    readerTitle = title,
                    readerParagraphs = paragraphs
                )
            } else tab
        }
    }

    // --- Gemini Summarization Engine ---
    fun summarizePage(title: String, contentText: String) {
        if (contentText.isBlank() || contentText.length < 50) {
            _aiError.value = "The webpage has too little text content to generate an AI summary."
            return
        }

        viewModelScope.launch {
            _aiLoading.value = true
            _aiSummary.value = null
            _aiError.value = null

            val previewContent = if (contentText.length > 8000) contentText.take(8000) + "..." else contentText
            val prompt = """
                Webpage Title: $title
                Webpage Text Content: $previewContent
                
                Please generate an elite, professional, bulleted executive summary of this webpage. Extract the key takeaways, structural summaries, and highlight critical insights. Format beautifully and professionally.
            """.trimIndent()

            try {
                val systemPrompt = "You are Apex Sidekick, an elite AI web research assistant built into the Apex Browser app. Communicate clearly and avoid generic fluff or technical jargon."
                val summaryResult = GeminiClient.queryGemini(prompt, systemPrompt)
                _aiSummary.value = summaryResult
            } catch (e: Exception) {
                _aiError.value = "Failed to compile summary: ${e.localizedMessage}"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun clearAiState() {
        _aiSummary.value = null
        _aiLoading.value = false
        _aiError.value = null
    }

    // --- Utility helper tools ---
    fun sanitizeUrl(input: String): String {
        var url = input.trim()
        if (url.isEmpty()) return "homepage"

        // If it looks like a URL, add http/https if missing
        val isDomain = url.contains(".") && !url.contains(" ")
        if (isDomain) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            return url
        }

        // Otherwise handle it as a search term
        val queryUrl = when (_searchEngine.value) {
            "duckduckgo" -> "https://duckduckgo.com/?q="
            "bing" -> "https://www.bing.com/search?q="
            "ecosia" -> "https://www.ecosia.org/search?q="
            else -> "https://www.google.com/search?q="
        }
        return queryUrl + java.net.URLEncoder.encode(url, "UTF-8")
    }
}
