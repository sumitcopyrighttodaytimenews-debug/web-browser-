package com.example.ui

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Bookmark
import com.example.data.HistoryItem
import com.example.ui.theme.BrowserThemeId
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.TabState
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMainScreen(viewModel: BrowserViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val themeId by viewModel.theme.collectAsStateWithLifecycle()
    val isBarBottom by viewModel.addressBarBottom.collectAsStateWithLifecycle()
    val adBlockEnabled by viewModel.adBlockerEnabled.collectAsStateWithLifecycle()
    val tabsList by viewModel.tabs.collectAsStateWithLifecycle()
    val isIncognito by viewModel.isIncognito.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val currentContext = LocalContext.current

    // Modal panel states
    var showTabsSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showBookmarksHistorySheet by remember { mutableStateOf(false) }
    var showAiCompanionSheet by remember { mutableStateOf(false) }

    // Dialog for adding customSpeed Dial shortcut
    var showAddShortcutDialog by remember { mutableStateOf(false) }

    val activeWebView = remember { mutableStateOf<WebView?>(null) }

    // Double Back tap handler to close tabs elegantly
    BackHandler(enabled = activeTab?.canGoBack == true || activeTab?.currentUrl != "homepage") {
        if (activeTab?.canGoBack == true) {
            activeWebView.value?.goBack()
        } else if (activeTab?.currentUrl != "homepage") {
            viewModel.updateTabUrlState(
                tabId = activeTab?.id ?: "",
                url = "homepage",
                title = "New Tab",
                canGoBack = false,
                canGoForward = false
            )
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        topBar = {
            if (!isBarBottom) {
                BrowserHeader(
                    activeTab = activeTab,
                    tabsCount = tabsList.size,
                    isIncognito = isIncognito,
                    onSearchSubmit = { viewModel.updateTabUrlState(activeTab?.id ?: "", viewModel.sanitizeUrl(it), "Loading...", false, false) },
                    onTabBadgeClick = { showTabsSheet = true },
                    onSettingsClick = { showSettingsSheet = true },
                    onToggleBookmark = {
                        activeTab?.let { tab ->
                            viewModel.toggleBookmark(tab.title, tab.currentUrl)
                        }
                    },
                    isBookmarked = viewModel.bookmarks.collectAsStateWithLifecycle().value.any { it.url == activeTab?.currentUrl },
                    onAiSparkClick = {
                        activeWebView.value?.let { wv ->
                            wv.evaluateJavascript(
                                "(function() { return document.body.innerText; })()",
                                { textResult ->
                                    // Parse double-quoted string response from JS evaluator
                                    val cleanedText = if (textResult.startsWith("\"") && textResult.endsWith("\"")) {
                                        try {
                                            JSONObject("{ \"text\": $textResult }").getString("text")
                                        } catch (e: Exception) {
                                            textResult.trim('"')
                                        }
                                    } else {
                                        textResult
                                    }
                                    viewModel.summarizePage(activeTab?.title ?: "Web Page", cleanedText)
                                }
                            )
                        }
                        showAiCompanionSheet = true
                    },
                    onRefresh = { activeWebView.value?.reload() }
                )
            }
        },
        bottomBar = {
            Column {
                if (isBarBottom) {
                    BrowserHeader(
                        activeTab = activeTab,
                        tabsCount = tabsList.size,
                        isIncognito = isIncognito,
                        onSearchSubmit = { viewModel.updateTabUrlState(activeTab?.id ?: "", viewModel.sanitizeUrl(it), "Loading...", false, false) },
                        onTabBadgeClick = { showTabsSheet = true },
                        onSettingsClick = { showSettingsSheet = true },
                        onToggleBookmark = {
                            activeTab?.let { tab ->
                                viewModel.toggleBookmark(tab.title, tab.currentUrl)
                            }
                        },
                        isBookmarked = viewModel.bookmarks.collectAsStateWithLifecycle().value.any { it.url == activeTab?.currentUrl },
                        onAiSparkClick = {
                            activeWebView.value?.let { wv ->
                                wv.evaluateJavascript(
                                    "(function() { return document.body.innerText; })()",
                                    { textResult ->
                                        val cleanedText = if (textResult.startsWith("\"") && textResult.endsWith("\"")) {
                                            try {
                                                JSONObject("{ \"text\": $textResult }").getString("text")
                                            } catch (e: Exception) {
                                                textResult.trim('"')
                                            }
                                        } else {
                                            textResult
                                        }
                                        viewModel.summarizePage(activeTab?.title ?: "Web Page", cleanedText)
                                    }
                                )
                            }
                            showAiCompanionSheet = true
                        },
                        onRefresh = { activeWebView.value?.reload() }
                    )
                }

                // Global Status Navigation Controls Bar
                BottomNavigationControls(
                    canGoBack = activeTab?.canGoBack == true,
                    canGoForward = activeTab?.canGoForward == true,
                    onBack = { activeWebView.value?.goBack() },
                    onForward = { activeWebView.value?.goForward() },
                    onHome = {
                        viewModel.updateTabUrlState(
                            tabId = activeTab?.id ?: "",
                            url = "homepage",
                            title = "New Tab",
                            canGoBack = false,
                            canGoForward = false
                        )
                    },
                    onBookmarksHistoryClick = { showBookmarksHistorySheet = true },
                    onReaderToggle = {
                        activeTab?.let { tab ->
                            if (tab.isReaderMode) {
                                viewModel.setReaderMode(tab.id, false)
                            } else {
                                activeWebView.value?.evaluateJavascript(
                                    """
                                    (function() {
                                        var paragraphs = [];
                                        var pElements = document.getElementsByTagName("p");
                                        for(var i=0; i<pElements.length; i++) {
                                            var txt = pElements[i].innerText.trim();
                                            if (txt.length > 25) { paragraphs.push(txt); }
                                        }
                                        return JSON.stringify({
                                            title: document.title || "",
                                            paragraphs: paragraphs
                                        });
                                    })()
                                    """.trimIndent(),
                                    { reply ->
                                        try {
                                            val rawJson = if (reply.startsWith("\"") && reply.endsWith("\"")) {
                                                JSONObject("{ \"data\": $reply }").getString("data")
                                            } else {
                                                reply
                                            }
                                            val obj = JSONObject(rawJson)
                                            val title = obj.optString("title", tab.title)
                                            val jsonArr = obj.optJSONArray("paragraphs")
                                            val paragraphsList = mutableListOf<String>()
                                            if (jsonArr != null) {
                                                for (j in 0 until jsonArr.length()) {
                                                    paragraphsList.add(jsonArr.getString(j))
                                                }
                                            }
                                            viewModel.setReaderMode(
                                                tabId = tab.id,
                                                enabled = true,
                                                title = title,
                                                paragraphs = paragraphsList
                                            )
                                        } catch (e: Exception) {
                                            viewModel.setReaderMode(
                                                tabId = tab.id,
                                                enabled = true,
                                                title = "Extracted Article",
                                                paragraphs = listOf("Failed to auto-parse reading text. Tap exit to return to normal web rendering.")
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    },
                    isReaderActive = activeTab?.isReaderMode == true,
                    isHomepage = activeTab?.currentUrl == "homepage"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            activeTab?.let { tab ->
                when {
                    tab.currentUrl == "homepage" -> {
                        HomepageDashboard(
                            isIncognito = isIncognito,
                            viewModel = viewModel,
                            onAddShortcutClick = { showAddShortcutDialog = true },
                            onTriggerAccentChrome = { showSettingsSheet = true },
                            onTriggerActiveStack = { showTabsSheet = true },
                            onTriggerRecentFlows = { showBookmarksHistorySheet = true },
                            onTriggerNewWorkspace = { viewModel.createNewTab("homepage", isIncognito) }
                        )
                    }
                    tab.isReaderMode -> {
                        ReaderModeConsole(tab = tab, onExit = { viewModel.setReaderMode(tab.id, false) })
                    }
                    else -> {
                        // The actual customizable WebView
                        WebRenderingCanvas(
                            tab = tab,
                            viewModel = viewModel,
                            activeWebView = activeWebView,
                            adBlockEnabled = adBlockEnabled
                        )
                    }
                }
            }
        }
    }

    // Sheet layouts
    if (showTabsSheet) {
        TabsSwitcherSheet(
            tabsList = tabsList,
            activeTabId = viewModel.activeTabId.collectAsStateWithLifecycle().value,
            onClose = { showTabsSheet = false },
            onSelectTab = {
                viewModel.switchActiveTab(it)
                showTabsSheet = false
            },
            onCloseTab = { viewModel.closeTab(it) },
            onCreateTab = { incognito ->
                viewModel.createNewTab("homepage", incognito)
                showTabsSheet = false
            }
        )
    }

    if (showSettingsSheet) {
        SettingsEditorSheet(
            themeId = themeId,
            isBarBottom = isBarBottom,
            searchEngine = viewModel.searchEngine.collectAsStateWithLifecycle().value,
            adBlocker = adBlockEnabled,
            fontScale = viewModel.fontScale.collectAsStateWithLifecycle().value,
            desktopMode = viewModel.desktopMode.collectAsStateWithLifecycle().value,
            jsEnabled = viewModel.javascriptEnabled.collectAsStateWithLifecycle().value,
            onClose = { showSettingsSheet = false },
            onUpdateTheme = { viewModel.updateTheme(it) },
            onToggleBarLayout = { viewModel.toggleAddressBarLayout() },
            onUpdateSearch = { viewModel.updateSearchEngine(it) },
            onToggleAdBlock = { viewModel.toggleAdBlocker() },
            onUpdateFontScale = { viewModel.updateFontScale(it) },
            onToggleDesktop = { viewModel.toggleDesktopMode() },
            onToggleJs = { viewModel.toggleJavaScript() },
            onClearHistory = {
                viewModel.clearHistory()
                viewModel.clearSavedTabs()
            }
        )
    }

    if (showBookmarksHistorySheet) {
        BookmarksHistorySheet(
            bookmarks = viewModel.bookmarks.collectAsStateWithLifecycle().value,
            history = viewModel.history.collectAsStateWithLifecycle().value,
            onClose = { showBookmarksHistorySheet = false },
            onNavigateUrl = { url ->
                activeTab?.let { tab ->
                    viewModel.updateTabUrlState(tab.id, url, "Loading...", false, false)
                }
                showBookmarksHistorySheet = false
            },
            onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
            onClearHistory = { viewModel.clearHistory() },
            onRemoveBookmark = { viewModel.toggleBookmark("", it) }
        )
    }

    if (showAiCompanionSheet) {
        AiSidekickCompanionSheet(
            summary = viewModel.aiSummary.collectAsStateWithLifecycle().value,
            isLoading = viewModel.aiLoading.collectAsStateWithLifecycle().value,
            error = viewModel.aiError.collectAsStateWithLifecycle().value,
            onClose = {
                viewModel.clearAiState()
                showAiCompanionSheet = false
            }
        )
    }

    if (showAddShortcutDialog) {
        AddShortcutDialog(
            onDismiss = { showAddShortcutDialog = false },
            onAdd = { label, url ->
                viewModel.toggleBookmark(label, viewModel.sanitizeUrl(url))
                showAddShortcutDialog = false
            }
        )
    }
}

// --- Composable Subcomponents ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserHeader(
    activeTab: TabState?,
    tabsCount: Int,
    isIncognito: Boolean,
    onSearchSubmit: (String) -> Unit,
    onTabBadgeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    isBookmarked: Boolean,
    onAiSparkClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var textState by remember(activeTab?.currentUrl) {
        mutableStateOf(if (activeTab?.currentUrl == "homepage") "" else activeTab?.currentUrl ?: "")
    }

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 4.dp,
        color = if (isIncognito) Color(0xFF1E1E24) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            // Linear loading indicator
            if (activeTab != null && activeTab.loadingProgress in 1..99 && activeTab.currentUrl != "homepage") {
                LinearProgressIndicator(
                    progress = { activeTab.loadingProgress / 100f },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SSL lock indicator or incognito shield
                Icon(
                    imageVector = if (isIncognito) Icons.Default.PrivacyTip else if (activeTab?.currentUrl?.startsWith("https://") == true) Icons.Default.Lock else Icons.Default.Info,
                    contentDescription = "Security Rating",
                    tint = if (isIncognito) MaterialTheme.colorScheme.secondary else if (activeTab?.currentUrl?.startsWith("https://") == true) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 6.dp, end = 4.dp).size(18.dp)
                )

                // Editable Address Card Field
                TextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = {
                        Text(
                            text = if (isIncognito) "Secret Private Search" else "Browse or key search",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("address_bar_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = if (isIncognito) Color.White else MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = if (isIncognito) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface
                    ),
                    textStyle = TextStyle(fontSize = 14.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (textState.isNotBlank()) {
                            onSearchSubmit(textState)
                        }
                        focusManager.clearFocus()
                    }),
                    trailingIcon = {
                        if (textState.isNotEmpty()) {
                            IconButton(
                                onClick = { textState = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear address text",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )

                // Top Bar Operations Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    if (activeTab?.currentUrl != "homepage" && activeTab != null) {
                        // AI companion shortcut trigger button
                        IconButton(
                            onClick = onAiSparkClick,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("ai_star_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini Companion Sidekick",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("bookmark_star_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Page",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // TABS badges button showing tab number
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTabBadgeClick() }
                            .testTag("tab_badges_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .border(2.dp, if (isIncognito) Color.White else MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabsCount.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncognito) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Settings trigger options dots
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("settings_dots_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More custom settings",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationControls(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onBookmarksHistoryClick: () -> Unit,
    onReaderToggle: () -> Unit,
    isReaderActive: Boolean,
    isHomepage: Boolean
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                enabled = canGoBack,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go Back",
                    tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            IconButton(
                onClick = onForward,
                enabled = canGoForward,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go Forward",
                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            IconButton(
                onClick = onHome,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home Page"
                )
            }

            IconButton(
                onClick = onBookmarksHistoryClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmarks,
                    contentDescription = "Saved Bookmarks and Browsing History"
                )
            }

            // Reader Mode quick action
            IconButton(
                onClick = onReaderToggle,
                enabled = !isHomepage,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("reader_mode_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChromeReaderMode,
                    contentDescription = "Read content cleanly",
                    tint = if (isReaderActive) MaterialTheme.colorScheme.primary else if (!isHomepage) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun HomepageDashboard(
    isIncognito: Boolean,
    viewModel: BrowserViewModel,
    onAddShortcutClick: () -> Unit,
    onTriggerAccentChrome: () -> Unit,
    onTriggerActiveStack: () -> Unit,
    onTriggerRecentFlows: () -> Unit,
    onTriggerNewWorkspace: () -> Unit
) {
    val currentTheme by viewModel.theme.collectAsStateWithLifecycle()
    val searchEngine by viewModel.searchEngine.collectAsStateWithLifecycle()
    val bookmarksList by viewModel.bookmarks.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var searchText by remember { mutableStateOf("") }

    // Derive speed dial items from bookmarks starting with simple descriptions or direct lists
    val defaultShortcuts = listOf(
        Pair("Google", "https://www.google.com"),
        Pair("Wikipedia", "https://www.wikipedia.org"),
        Pair("YouTube", "https://www.youtube.com"),
        Pair("Github", "https://github.com"),
        Pair("Reddit", "https://www.reddit.com"),
        Pair("TechCrunch", "https://techcrunch.com"),
        Pair("BBC News", "https://www.bbc.com/news"),
        Pair("Vico Charts", "https://github.com/patrykandpatryky/vico")
    )

    // Blend user custom bookmarks into dial shortcuts
    val displayShortcuts = remember(bookmarksList) {
        val lists = defaultShortcuts.toMutableList()
        bookmarksList.forEach { bk ->
            if (lists.none { it.second == bk.url }) {
                lists.add(Pair(bk.title.take(15), bk.url))
            }
        }
        lists
    }

    // Dynamic graphic background brush representing customization aesthetic values
    val meshBgBrush = remember(currentTheme) {
        when (currentTheme) {
            BrowserThemeId.BOLD_TYPOGRAPHY -> Brush.verticalGradient(listOf(Color(0xFFFEF7FF), Color(0xFFF5ECF6)))
            BrowserThemeId.CYBERPUNK -> Brush.verticalGradient(listOf(Color(0xFF0C0714), Color(0xFF280B44)))
            BrowserThemeId.ARCTIC -> Brush.linearGradient(listOf(Color(0xFFE0F2F1), Color(0xFFFFFFFF)))
            BrowserThemeId.SAKURA -> Brush.verticalGradient(listOf(Color(0xFFFFEFF6), Color(0xFFFFC0D9)))
            BrowserThemeId.FOREST -> Brush.linearGradient(listOf(Color(0xFF142017), Color(0xFF2E4E30)))
            BrowserThemeId.SUNSET -> Brush.verticalGradient(listOf(Color(0xFF1D0E25), Color(0xFF5E1B25)))
            BrowserThemeId.AMOLED -> Brush.verticalGradient(listOf(Color.Black, Color.Black))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(meshBgBrush)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (currentTheme == BrowserThemeId.BOLD_TYPOGRAPHY) Alignment.Start else Alignment.CenterHorizontally
        ) {
            if (currentTheme == BrowserThemeId.BOLD_TYPOGRAPHY) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Bold Typography Theme Hero layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "YOUR\nSPACE",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 50.sp,
                        letterSpacing = (-2).sp,
                        color = Color(0xFF21005D),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Fully adapted to your focus mode.",
                        color = Color(0xFF49454F),
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Spacer(modifier = Modifier.height(40.dp))

                // Premium custom typography display banner
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "APEX",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 8.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "C U S T O M I Z A B L E   W E B   P I L O T",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(35.dp))
            }

            // Large center quick search widget
            Card(
                shape = if (currentTheme == BrowserThemeId.BOLD_TYPOGRAPHY) RoundedCornerShape(28.dp) else RoundedCornerShape(22.dp),
                border = BorderStroke(
                    if (currentTheme == BrowserThemeId.BOLD_TYPOGRAPHY) 1.dp else 1.5.dp,
                    if (currentTheme == BrowserThemeId.BOLD_TYPOGRAPHY) Color(0xFFCAC4D0) else MaterialTheme.colorScheme.primary
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentTheme == BrowserThemeId.BOLD_TYPOGRAPHY) Color(0xFFF3EDF7) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImageVectorForEngine(searchEngine, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))

                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = {
                            Text(
                                if (currentTheme == BrowserThemeId.BOLD_TYPOGRAPHY) "search.zen.browser/home" else "Navigate securely or search with ${searchEngine.replaceFirstChar { it.uppercase() }}",
                                fontSize = 14.sp,
                                maxLines = 1,
                                color = if (currentTheme == BrowserThemeId.BOLD_TYPOGRAPHY) Color(0xFF49454F) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_search_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = if (currentTheme == BrowserThemeId.BOLD_TYPOGRAPHY) Color(0xFF1D1B20) else MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (searchText.isNotBlank()) {
                                viewModel.activeTab.value?.let { tab ->
                                    viewModel.updateTabUrlState(tab.id, viewModel.sanitizeUrl(searchText), "Loading...", false, false)
                                }
                            }
                            focusManager.clearFocus()
                        })
                    )

                    IconButton(
                        onClick = {
                            if (searchText.isNotBlank()) {
                                viewModel.activeTab.value?.let { tab ->
                                    viewModel.updateTabUrlState(tab.id, viewModel.sanitizeUrl(searchText), "Loading...", false, false)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (currentTheme == BrowserThemeId.BOLD_TYPOGRAPHY) Color(0xFF49454F) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (currentTheme == BrowserThemeId.BOLD_TYPOGRAPHY) {
                Spacer(modifier = Modifier.height(24.dp))

                // Quick Customization Grid (Material 3 Cards)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: ACCENT CHROME
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clickable { onTriggerAccentChrome() },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Palette accent customization code",
                                    tint = Color(0xFF21005D),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "ACCENT\nCHROME",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 16.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color(0xFF21005D)
                                )
                            }
                        }

                        // Card 2: ACTIVE STACK
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clickable { onTriggerActiveStack() },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD0BCFF))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Active windows stack",
                                    tint = Color(0xFF381E72),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "ACTIVE\nSTACK",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 16.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color(0xFF381E72)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 3: RECENT FLOWS
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clickable { onTriggerRecentFlows() },
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Recent flows browsing history",
                                    tint = Color(0xFF49454F),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "RECENT\nFLOWS",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 16.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color(0xFF49454F)
                                )
                            }
                        }

                        // Card 4: NEW WORKSPACE
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clickable { onTriggerNewWorkspace() },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF21005D))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Launch secondary workspace panel",
                                    tint = Color(0xFFEADDFF),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "NEW\nWORKSPACE",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 16.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color(0xFFEADDFF)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Feature Chips (Horizontal Overflow Simulation)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip 1: AI SUMMARIZER
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8DEF8), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFD0BCFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "AI SUMMARIZER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D192B)
                        )
                    }

                    // Chip 2: AD BLOCKER
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3EDF7), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "AD BLOCKER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF49454F)
                        )
                    }

                    // Chip 3: PIP MODE
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3EDF7), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "PIP MODE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF49454F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }

            // Privacy mode notice
            if (isIncognito) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1C3C)),
                    border = BorderStroke(1.dp, Color(0xFFFF007F))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Incognito Mode",
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Incognito Browsing Active", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Apex is keeping your activity locked away. No local database cache, history, cookies, or searches will be accumulated.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Quick Bookmarks / Dial grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed Dial Link Studio",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = onAddShortcutClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "New shortcut dial link", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Custom grid items for high customization dialing
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(displayShortcuts) { shortcut ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.activeTab.value?.let { tab ->
                                    viewModel.updateTabUrlState(tab.id, shortcut.second, shortcut.first, false, false)
                                }
                            }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shortcut.first.take(2).uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = shortcut.first,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageVectorForEngine(engineName: String, modifier: Modifier = Modifier) {
    val pair = when (engineName) {
        "duckduckgo" -> Pair(Icons.Default.Pets, Color(0xFFF57C00))
        "bing" -> Pair(Icons.Default.TravelExplore, Color(0xFF0078D7))
        "ecosia" -> Pair(Icons.Default.NaturePeople, Color(0xFF4CAF50))
        else -> Pair(Icons.Default.Language, Color(0xFF4285F4))
    }
    val vector = pair.first
    val tint = pair.second
    Icon(imageVector = vector, contentDescription = "$engineName engine", tint = tint, modifier = modifier)
}

@Composable
fun WebRenderingCanvas(
    tab: TabState,
    viewModel: BrowserViewModel,
    activeWebView: MutableState<WebView?>,
    adBlockEnabled: Boolean
) {
    val context = LocalContext.current
    val systemJsEnabled by viewModel.javascriptEnabled.collectAsStateWithLifecycle()
    val systemDesktopMode by viewModel.desktopMode.collectAsStateWithLifecycle()

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .testTag("webview_renderer"),
        factory = { ctx ->
            WebView(ctx).apply {
                // Configure optimal security & caching specs
                settings.apply {
                    javaScriptEnabled = systemJsEnabled
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false // Let WebView render navigation directly in the stack
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let {
                            viewModel.updateTabUrlState(tab.id, it, "Loading...", canGoBack(), canGoForward())
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val title = view?.title ?: url ?: "A Web Page"
                        url?.let {
                            viewModel.updateTabUrlState(tab.id, it, title, canGoBack(), canGoForward())
                            // Record page visit history (skip incognito)
                            viewModel.addHistory(title, it, tab.isIncognito)
                        }
                    }

                    // Raw ad blocker network interceptor using Moshi ad keywords block
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val requestUrl = request?.url?.toString() ?: return null
                        if (adBlockEnabled) {
                            val maliciousBlockers = listOf(
                                "doubleclick.net", "googleadservices.com", "googlesyndication.com",
                                "pagead2", "adnxs.com", "applovin.com", "vungle.com", "flurry.com",
                                "quantserve", "serving-sys", "adservice.google"
                            )
                            for (item in maliciousBlockers) {
                                if (requestUrl.contains(item)) {
                                    return WebResourceResponse(
                                        "text/javascript",
                                        "UTF-8",
                                        ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        viewModel.updateTabLoadingProgress(tab.id, newProgress)
                    }
                }

                activeWebView.value = this
                loadUrl(tab.currentUrl)
            }
        },
        update = { webView ->
            // Sync mutable settings attributes dynamically
            webView.settings.javaScriptEnabled = systemJsEnabled
            if (systemDesktopMode) {
                webView.settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Safari/537.36"
                webView.settings.useWideViewPort = true
                webView.settings.loadWithOverviewMode = true
            } else {
                webView.settings.userAgentString = null // Default mobile agent
            }

            // Execute dynamic page loading on safe switches
            if (webView.url != tab.currentUrl && tab.currentUrl != "homepage") {
                webView.loadUrl(tab.currentUrl)
            }
        }
    )
}

@Composable
fun ReaderModeConsole(
    tab: TabState,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Badge(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text("Reader Mode Console", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onExit,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Reader Screen", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Exit Reader", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = tab.readerTitle.ifEmpty { "Readable Document Content" },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))

        if (tab.readerParagraphs.isEmpty()) {
            Text(
                "Could not extract body blocks for this address safely. Tap Exit Reader above to navigate standard elements.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 40.dp)
            )
        } else {
            tab.readerParagraphs.forEach { txt ->
                Text(
                    text = txt,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

// --- Sheets, Drawer Overlays ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsSwitcherSheet(
    tabsList: List<TabState>,
    activeTabId: String,
    onClose: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCreateTab: (Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Browsing Hub Windows",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss Sheet")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action links for new tabs creation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onCreateTab(false) },
                    modifier = Modifier.weight(1f).testTag("new_standard_tab_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New Tab")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Standard Window", fontSize = 12.sp)
                }

                Button(
                    onClick = { onCreateTab(true) },
                    modifier = Modifier.weight(1f).testTag("new_private_tab_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF311B92))
                ) {
                    Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = "New Incognito Tab")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Incognito Safe", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // List of active tabs
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tabsList) { tab ->
                    val isActive = tab.id == activeTabId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTab(tab.id) },
                        border = BorderStroke(
                            1.5.dp,
                            if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (tab.isIncognito) Color(0xFF1F1A24) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (tab.isIncognito) Icons.Default.Security else Icons.Default.Web,
                                contentDescription = "Window Type",
                                tint = if (tab.isIncognito) Color(0xFF00FFCC) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tab.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (tab.isIncognito) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (tab.currentUrl == "homepage") "Launch Speed dials" else tab.currentUrl,
                                    fontSize = 11.sp,
                                    color = if (tab.isIncognito) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { onCloseTab(tab.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Terminate tab dialog",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsEditorSheet(
    themeId: BrowserThemeId,
    isBarBottom: Boolean,
    searchEngine: String,
    adBlocker: Boolean,
    fontScale: Float,
    desktopMode: Boolean,
    jsEnabled: Boolean,
    onClose: () -> Unit,
    onUpdateTheme: (BrowserThemeId) -> Unit,
    onToggleBarLayout: () -> Unit,
    onUpdateSearch: (String) -> Unit,
    onToggleAdBlock: () -> Unit,
    onUpdateFontScale: (Float) -> Unit,
    onToggleDesktop: () -> Unit,
    onToggleJs: () -> Unit,
    onClearHistory: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Theme & Setting Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Sheet")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // --- 1. Dynamic Presets Theme Selector ---
            Text("Aesthetic Vibe Profile", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrowserThemeId.values().forEach { id ->
                    val isSelected = id == themeId
                    SuggestionChip(
                        onClick = { onUpdateTheme(id) },
                        label = { Text(id.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        border = BorderStroke(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 2. Advanced Custom Controls Group ---
            Text("Engine Customizations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // Adblock Toggle custom control row
            CustomSettingSwitchRow(
                icon = Icons.Default.Shield,
                title = "Apex Secure Ad-Blocker",
                description = "Blocks ads, tracking payloads, and redirects dynamically.",
                checked = adBlocker,
                onCheckedChange = { onToggleAdBlock() }
            )

            // Address bar bottom switch
            CustomSettingSwitchRow(
                icon = Icons.Default.Compress,
                title = "Address Bar at the Bottom",
                description = "Moves URL bar to bottom for standard simple one-handed use.",
                checked = isBarBottom,
                onCheckedChange = { onToggleBarLayout() }
            )

            // JavaScript Enable/Disable security toggle
            CustomSettingSwitchRow(
                icon = Icons.Default.Code,
                title = "Enable Javascript Execution",
                description = "Run scripts. Turn off to avoid page tracking scripts completely.",
                checked = jsEnabled,
                onCheckedChange = { onToggleJs() }
            )

            // Desktop Mode User Agent Toggle
            CustomSettingSwitchRow(
                icon = Icons.Default.DesktopMac,
                title = "Desktop Simulation Agent",
                description = "Always requests full desktop version pages of websites.",
                checked = desktopMode,
                onCheckedChange = { onToggleDesktop() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Search engine choices
            Text("Search Engine Gateway", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("google", "duckduckgo", "bing", "ecosia").forEach { engine ->
                    val isSelected = engine == searchEngine
                    Button(
                        onClick = { onUpdateSearch(engine) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(engine.replaceFirstChar { it.uppercase() }, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Text Font Scaling factor
            Text("Text size resolution (${(fontScale * 100).toInt()}%)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Slider(
                value = fontScale,
                onValueChange = { scale ->
                    val roundedValue = when {
                        scale < 0.9f -> 0.8f
                        scale < 1.1f -> 1.0f
                        scale < 1.3f -> 1.2f
                        else -> 1.4f
                    }
                    onUpdateFontScale(roundedValue)
                },
                valueRange = 0.8f..1.4f,
                steps = 2
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Danger Clear settings
            Button(
                onClick = {
                    onClearHistory()
                    onClose()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Wipe browser cache data")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Wipe History & Active Window Sessions", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun CustomSettingSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("switch_${title.replace(" ", "_").lowercase()}")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksHistorySheet(
    bookmarks: List<Bookmark>,
    history: List<HistoryItem>,
    onClose: () -> Unit,
    onNavigateUrl: (String) -> Unit,
    onDeleteHistoryItem: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveBookmark: (String) -> Unit
) {
    var showBookmarksTab by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Library Drawer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close library")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub Tab selector buttons
            TabRow(selectedTabIndex = if (showBookmarksTab) 0 else 1) {
                Tab(
                    selected = showBookmarksTab,
                    onClick = { showBookmarksTab = true },
                    text = { Text("Bookmarks (${bookmarks.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = !showBookmarksTab,
                    onClick = { showBookmarksTab = false },
                    text = { Text("History (${history.size})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showBookmarksTab) {
                if (bookmarks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Text("No saved bookmarks yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bookmarks) { bookmark ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateUrl(bookmark.url) }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Star, contentDescription = "Bookmarked", tint = Color(0xFFFFC107))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bookmark.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(bookmark.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { onRemoveBookmark(bookmark.url) }, modifier = Modifier.size(24.dp)) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove Bookmark", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Text("No browse patterns stored.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onClearHistory) {
                                Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Wipe historical logs", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear History Database", fontSize = 11.sp)
                            }
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(history) { log ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateUrl(log.url) }
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.History, contentDescription = "Browsing history", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(log.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(log.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        IconButton(onClick = { onDeleteHistoryItem(log.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Delete item", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSidekickCompanionSheet(
    summary: String?,
    isLoading: Boolean,
    error: String?,
    onClose: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color(0xFF0F0F1A) // Deep cosmic visual terminal
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Apex Cosmic companion",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "APEX SIDEKICK AI",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close panel")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF1F1B3E))

            when {
                isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00FFCC),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Analyzing layout, distilling content metadata and querying Gemini API...",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Error icon", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(error, fontSize = 13.sp, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                }

                summary != null -> {
                    Badge(
                        containerColor = Color(0xFF1C003D),
                        contentColor = Color(0xFF00FFCC),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text("PAGE SUMMARY INSIGHTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontFamily = FontFamily.Monospace)
                    }

                    Text(
                        text = summary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFFEEEEEE),
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    )
                }

                else -> {
                    Text(
                        "Tap 'AI' on any website to extract key research summaries recursively.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AddShortcutDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var labelText by remember { mutableStateOf("") }
    var urlText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Speed Dial Shortcut", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = { Text("Label (e.g. Google)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("URL Address (e.g. google.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (labelText.isNotBlank() && urlText.isNotBlank()) {
                        onAdd(labelText, urlText)
                    }
                }
            ) {
                Text("Save Shortcut")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}
