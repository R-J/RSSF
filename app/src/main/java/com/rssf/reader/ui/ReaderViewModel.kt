package com.rssf.reader.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rssf.reader.data.Category
import com.rssf.reader.data.Entry
import com.rssf.reader.data.Feed
import com.rssf.reader.data.ReaderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReaderViewModel(private val repository: ReaderRepository) : ViewModel() {
    private val _authorized = MutableStateFlow<Boolean?>(null)
    val authorized: StateFlow<Boolean?> = _authorized.asStateFlow()
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    private val _feeds = MutableStateFlow<List<Feed>>(emptyList())
    val feeds: StateFlow<List<Feed>> = _feeds.asStateFlow()
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init { viewModelScope.launch { _authorized.value = repository.isAuthorized(); if (_authorized.value == true) refresh() } }

    fun login(username: String, password: String) = viewModelScope.launch {
        runCatching { repository.login(username, password); _authorized.value = true; refresh() }
            .onFailure { _message.value = it.message ?: "Unable to sign in" }
    }

    fun logout() = viewModelScope.launch { repository.logout(); _authorized.value = false }
    fun refresh() = viewModelScope.launch {
        runCatching {
            _categories.value = repository.categories()
            _feeds.value = repository.feeds()
            _entries.value = repository.entries()
        }.onFailure { _message.value = "Offline: showing cached content" }
    }
    fun search(query: String) = viewModelScope.launch { if (query.isBlank()) refresh() else runCatching { _entries.value = repository.search(query) }.onFailure { _message.value = "Search failed" } }
    fun clearMessage() { _message.value = null }

    companion object {
        fun factory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ReaderViewModel(ReaderRepository(context)) as T
        }
    }
}
