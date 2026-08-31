package com.example.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.portal.ArticleDetail
import com.example.data.portal.ArticleSummary
import com.example.data.portal.AuthorRef
import com.example.data.portal.CategoryRef
import com.example.data.portal.CommentItem
import com.example.data.portal.HomeFeed
import com.example.data.portal.Page
import com.example.data.portal.PortalError
import com.example.data.portal.PortalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModels for the public reader.
 *
 * One rule runs through all of them: **the UI state is a single sealed value**,
 * so a screen is either loading, has content, or has an error — never two of
 * those at once. Paged screens additionally keep `isLoadingMore` and `total`
 * alongside a `Ready` state so a "load more" failure does not blank the list.
 */

private fun PortalError?.message(): String =
    (this as? PortalError)?.message ?: "তথ্য লোড করতে সমস্যা হয়েছে।"

// ---------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val feed: HomeFeed, val isRefreshing: Boolean = false) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: PortalRepository) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Set when the feed falls back to cached/stale data after a failure. */
    private val _offlineNotice = MutableStateFlow<String?>(null)
    val offlineNotice: StateFlow<String?> = _offlineNotice.asStateFlow()

    init { load() }

    fun load(force: Boolean = false) {
        viewModelScope.launch {
            val isRefresh = _state.value is HomeUiState.Ready
            if (isRefresh) {
                _state.update { (it as HomeUiState.Ready).copy(isRefreshing = true) }
            } else {
                _state.value = HomeUiState.Loading
            }

            repository.homeFeed()
                .onSuccess { feed ->
                    _state.value = HomeUiState.Ready(feed)
                    _offlineNotice.value = null
                }
                .onFailure { error ->
                    val portalError = error as? PortalError
                    val previous = (_state.value as? HomeUiState.Ready)?.feed
                    if (previous != null) {
                        // Keep showing what we have; pull-to-refresh just stops.
                        _state.value = HomeUiState.Ready(previous)
                        _offlineNotice.value = portalError.message()
                    } else {
                        _state.value = HomeUiState.Error(portalError.message())
                    }
                }
        }
    }
}

// ---------------------------------------------------------------------------
// Article reader
// ---------------------------------------------------------------------------

sealed interface ArticleUiState {
    data object Loading : ArticleUiState
    data class Ready(
        val article: ArticleDetail,
        val author: AuthorRef? = null,
        val comments: List<CommentItem> = emptyList(),
        val related: List<ArticleSummary> = emptyList(),
        val commentPosted: Boolean = false
    ) : ArticleUiState
    data class Error(val message: String) : ArticleUiState
}

class ArticleViewModel(private val repository: PortalRepository) : ViewModel() {

    private val _state = MutableStateFlow<ArticleUiState>(ArticleUiState.Loading)
    val state: StateFlow<ArticleUiState> = _state.asStateFlow()

    private val _commentStatus = MutableStateFlow<String?>(null)
    val commentStatus: StateFlow<String?> = _commentStatus.asStateFlow()

    private val _isPostingComment = MutableStateFlow(false)
    val isPostingComment: StateFlow<Boolean> = _isPostingComment.asStateFlow()

    private var currentIdOrSlug: String = ""

    fun retry() {
        if (currentIdOrSlug.isNotBlank()) {
            load(currentIdOrSlug)
        }
    }

    fun load(idOrSlug: String) {
        currentIdOrSlug = idOrSlug.trim()
        if (currentIdOrSlug.isBlank()) {
            _state.value = ArticleUiState.Error("প্রবন্ধটি পাওয়া যায়নি।")
            return
        }
        viewModelScope.launch {
            _state.value = ArticleUiState.Loading
            val detail = repository.article(currentIdOrSlug)

            if (detail.isFailure) {
                _state.value = ArticleUiState.Error((detail.exceptionOrNull() as? PortalError).message())
                return@launch
            }

            val article = detail.getOrThrow()
            _state.value = ArticleUiState.Ready(article)

            // Load author details if available
            launch {
                val authorId = article.summary.authorId
                if (!authorId.isNullOrBlank()) {
                    val authorRef = repository.author(authorId).getOrNull()
                    if (authorRef != null) {
                        _state.update { current ->
                            (current as? ArticleUiState.Ready)?.copy(author = authorRef) ?: current
                        }
                    }
                }
            }

            // Comments and related reading are secondary: fetch them together
            // and drop either one if it fails rather than failing the article.
            launch {
                val comments = repository.comments(article.id).getOrNull().orEmpty()
                _state.update { current ->
                    (current as? ArticleUiState.Ready)?.copy(comments = comments) ?: current
                }
            }
            launch {
                val related = relatedOf(article)
                _state.update { current ->
                    (current as? ArticleUiState.Ready)?.copy(related = related) ?: current
                }
            }
        }
    }

    /** Same category first, then same author, capped at four. */
    private suspend fun relatedOf(article: ArticleDetail): List<ArticleSummary> {
        val categoryId = article.summary.categoryId
        val authorId = article.summary.authorId
        val picks = mutableListOf<ArticleSummary>()
        if (!categoryId.isNullOrBlank()) {
            repository.articlesByCategory(categoryId, limit = 8)
                .getOrNull()?.items?.let { picks += it }
        }
        if (picks.size < 4 && !authorId.isNullOrBlank()) {
            repository.articlesByAuthor(authorId, limit = 8)
                .getOrNull()?.items?.let { picks += it }
        }
        return picks
            .filter { it.id != article.id }
            .distinctBy { it.id }
            .take(4)
    }

    fun postComment(name: String, email: String, content: String) {
        val current = (_state.value as? ArticleUiState.Ready) ?: return
        if (name.isBlank() || content.isBlank()) {
            _commentStatus.value = "নাম ও মন্তব্য আবশ্যক।"
            return
        }
        viewModelScope.launch {
            _isPostingComment.value = true
            _commentStatus.value = "মন্তব্য পাঠানো হচ্ছে..."
            repository.postComment(
                blogId = current.article.id,
                blogTitle = current.article.title,
                name = name,
                email = email,
                content = content
            ).onSuccess {
                _commentStatus.value = "মন্তব্য জমা হয়েছে। অনুমোদনের পর প্রকাশিত হবে।"
                _state.update { (it as? ArticleUiState.Ready)?.copy(commentPosted = true) ?: it }
            }.onFailure { error ->
                _commentStatus.value = (error as? PortalError).message()
            }
            _isPostingComment.value = false
        }
    }

    fun clearCommentStatus() { _commentStatus.value = null }
}

// ---------------------------------------------------------------------------
// Paged article lists (category, author, search)
// ---------------------------------------------------------------------------

sealed interface ListUiState {
    data object Loading : ListUiState
    data class Ready(
        val articles: List<ArticleSummary>,
        val total: Int? = null,
        val isLoadingMore: Boolean = false,
        val endReached: Boolean = false
    ) : ListUiState
    data class Error(val message: String) : ListUiState
}

/** Shared paging logic so category, author and search lists behave identically. */
private class ArticlePaginator(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val fetch: suspend (limit: Int, offset: Int) -> Result<Page<ArticleSummary>>
) {
    private val _state = MutableStateFlow<ListUiState>(ListUiState.Loading)
    val state: StateFlow<ListUiState> = _state.asStateFlow()

    fun loadFirst() {
        _state.value = ListUiState.Loading
        scope.launch {
            fetch(PAGE_SIZE, 0)
                .onSuccess { page ->
                    _state.value = ListUiState.Ready(
                        articles = page.items,
                        total = page.total,
                        endReached = page.items.size < PAGE_SIZE
                    )
                }
                .onFailure { _state.value = ListUiState.Error((it as? PortalError).message()) }
        }
    }

    fun loadMore() {
        val current = _state.value as? ListUiState.Ready ?: return
        if (current.isLoadingMore || current.endReached) return
        _state.value = current.copy(isLoadingMore = true)
        scope.launch {
            fetch(PAGE_SIZE, current.articles.size)
                .onSuccess { page ->
                    val merged = (current.articles + page.items).distinctBy { it.id }
                    _state.value = ListUiState.Ready(
                        articles = merged,
                        total = page.total ?: current.total,
                        endReached = page.items.isEmpty() || merged.size >= (page.total ?: Int.MAX_VALUE)
                    )
                }
                .onFailure {
                    _state.value = current.copy(isLoadingMore = false, endReached = true)
                }
        }
    }

    private companion object { const val PAGE_SIZE = 20 }
}

class CategoryViewModel(
    private val repository: PortalRepository,
    private val categorySlug: String
) : ViewModel() {

    private val _category = MutableStateFlow<CategoryRef?>(null)
    val category: StateFlow<CategoryRef?> = _category.asStateFlow()

    private val paginator = ArticlePaginator(viewModelScope) { limit, offset ->
        val id = _category.value?.id
            ?: repository.categoryBySlug(categorySlug).getOrNull()?.also { _category.value = it }?.id
        if (id == null) Result.failure(PortalError.NotFound)
        else repository.articlesByCategory(id, limit = limit, offset = offset)
    }
    val state: StateFlow<ListUiState> = paginator.state

    init { load() }

    fun load() {
        viewModelScope.launch {
            repository.categoryBySlug(categorySlug)
                .onSuccess { _category.value = it }
                .onFailure { _category.value = null }
            paginator.loadFirst()
        }
    }

    fun loadMore() = paginator.loadMore()
}

class AuthorViewModel(
    private val repository: PortalRepository,
    private val authorId: String
) : ViewModel() {

    private val _author = MutableStateFlow<AuthorRef?>(null)
    val author: StateFlow<AuthorRef?> = _author.asStateFlow()

    private val paginator = ArticlePaginator(viewModelScope) { limit, offset ->
        repository.articlesByAuthor(authorId, limit = limit, offset = offset)
    }
    val state: StateFlow<ListUiState> = paginator.state

    init { load() }

    fun load() {
        viewModelScope.launch {
            repository.author(authorId).onSuccess { _author.value = it }
            paginator.loadFirst()
        }
    }

    fun loadMore() = paginator.loadMore()
}

// ---------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------

class SearchViewModel(private val repository: PortalRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val paginator = ArticlePaginator(viewModelScope) { limit, offset ->
        repository.searchArticles(_query.value, limit = limit, offset = offset)
    }
    val state: StateFlow<ListUiState> = paginator.state

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryRef>>(emptyList())
    val categories: StateFlow<List<CategoryRef>> = _categories.asStateFlow()

    private var debounceJob: Job? = null

    init {
        viewModelScope.launch {
            _categories.value = repository.categories().getOrNull().orEmpty()
        }
        viewModelScope.launch {
            _suggestions.value = repository.categories().getOrNull()
                ?.take(8)?.map { it.title }
                .orEmpty()
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        debounceJob?.cancel()
        if (value.trim().length < 2) {
            _state_reset()
            return
        }
        debounceJob = viewModelScope.launch {
            delay(320) // debounce so we do not fire a request per keystroke
            paginator.loadFirst()
        }
    }

    fun submit(query: String) {
        debounceJob?.cancel()
        _query.value = query
        paginator.loadFirst()
    }

    fun loadMore() = paginator.loadMore()

    private fun _state_reset() {
        // Nothing to show before the term reaches two characters.
        viewModelScope.launch { paginator.loadFirst() }
    }
}

// ---------------------------------------------------------------------------
// Factory
// ---------------------------------------------------------------------------

class ReaderViewModelFactory(
    private val repository: PortalRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(repository) as T
        modelClass.isAssignableFrom(ArticleViewModel::class.java) ->
            ArticleViewModel(repository) as T
        modelClass.isAssignableFrom(SearchViewModel::class.java) ->
            SearchViewModel(repository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}

/** Keyed factories for the parameterised list screens. */
class CategoryViewModelFactory(
    private val repository: PortalRepository,
    private val categorySlug: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CategoryViewModel(repository, categorySlug) as T
}

class AuthorViewModelFactory(
    private val repository: PortalRepository,
    private val authorId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AuthorViewModel(repository, authorId) as T
}
