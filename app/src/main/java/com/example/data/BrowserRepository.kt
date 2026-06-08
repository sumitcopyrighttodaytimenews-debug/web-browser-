package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val dao: BrowserDao) {

    // --- Bookmarks methods ---
    val allBookmarks: Flow<List<Bookmark>> = dao.getAllBookmarks()

    suspend fun addBookmark(bookmark: Bookmark) {
        dao.insertBookmark(bookmark)
    }

    suspend fun removeBookmarkById(id: Int) {
        dao.deleteBookmarkById(id)
    }

    suspend fun removeBookmarkByUrl(url: String) {
        dao.deleteBookmarkByUrl(url)
    }

    fun isBookmarked(url: String): Flow<Boolean> {
        return dao.isBookmarked(url)
    }

    // --- History methods ---
    val allHistory: Flow<List<HistoryItem>> = dao.getAllHistory()

    fun searchHistory(query: String): Flow<List<HistoryItem>> {
        return dao.searchHistory(query)
    }

    suspend fun addHistoryItem(item: HistoryItem) {
        dao.insertHistoryItem(item)
    }

    suspend fun deleteHistoryById(id: Int) {
        dao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    // --- Tab methods ---
    val allSavedTabs: Flow<List<SavedTab>> = dao.getAllSavedTabs()

    suspend fun saveTab(tab: SavedTab) {
        dao.saveTab(tab)
    }

    suspend fun deleteTab(tab: SavedTab) {
        dao.deleteTab(tab)
    }

    suspend fun deleteTabById(tabId: String) {
        dao.deleteTabById(tabId)
    }

    suspend fun clearSavedTabs() {
        dao.clearSavedTabs()
    }
}
