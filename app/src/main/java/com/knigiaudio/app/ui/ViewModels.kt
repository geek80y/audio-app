package com.knigiaudio.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knigiaudio.app.data.BookDetails
import com.knigiaudio.app.data.BookShort
import com.knigiaudio.app.data.Genre
import com.knigiaudio.app.data.PageResult
import com.knigiaudio.app.data.Site
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Общая логика бесконечной ленты с подгрузкой страниц. */
abstract class PagedViewModel : ViewModel() {
    var items by mutableStateOf(listOf<BookShort>()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var hasMore by mutableStateOf(true); protected set

    private var page = 0
    private var job: Job? = null
    private var generation = 0

    protected abstract suspend fun load(page: Int): PageResult

    /** Очистить ленту без загрузки. */
    protected fun clear() {
        job?.cancel()
        generation++
        items = emptyList(); page = 0; hasMore = false; error = null; loading = false
    }

    /** Очистить и загрузить заново с первой страницы. */
    protected fun reset() {
        clear()
        hasMore = true
        loadMore()
    }

    fun retry() { error = null; loadMore() }

    fun loadMore() {
        if (loading || !hasMore || error != null) return
        loading = true
        val gen = generation
        job = viewModelScope.launch {
            try {
                val r = load(page + 1)
                if (gen != generation) return@launch
                page++
                items = (items + r.items).distinctBy { it.url }
                hasMore = r.hasMore && r.items.isNotEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen == generation) error = "Не удалось загрузить. Проверьте интернет."
            } finally {
                if (gen == generation) loading = false
            }
        }
    }
}

class FeedViewModel : PagedViewModel() {
    var genre by mutableStateOf<Genre?>(null); private set

    init { loadMore() }

    fun select(g: Genre?) {
        if (g == genre) return
        genre = g
        reset()
    }

    fun refresh() = reset()

    override suspend fun load(page: Int): PageResult =
        genre?.let { Site.genre(it.path, page) } ?: Site.latest(page)
}

class SearchViewModel : PagedViewModel() {
    var query by mutableStateOf(""); private set

    init { hasMore = false }

    fun submit(q: String) {
        val t = q.trim()
        if (t == query) return
        query = t
        if (t.length < 2) clear() else reset()
    }

    override suspend fun load(page: Int): PageResult =
        if (query.length < 2) PageResult(emptyList(), false) else Site.search(query, page)
}

sealed interface BookState {
    data object Loading : BookState
    data class Ok(val book: BookDetails) : BookState
    data class Error(val message: String) : BookState
}

class BookViewModel(private val url: String) : ViewModel() {
    var state by mutableStateOf<BookState>(BookState.Loading); private set

    init { load() }

    fun load() {
        state = BookState.Loading
        viewModelScope.launch {
            state = try {
                BookState.Ok(Site.details(url))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                BookState.Error("Не удалось открыть книгу. Проверьте интернет.")
            }
        }
    }
}
