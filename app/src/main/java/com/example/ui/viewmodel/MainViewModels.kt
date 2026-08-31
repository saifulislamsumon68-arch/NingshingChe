package com.example.ui.viewmodel

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.NinghsingCheAiAssistant
import com.example.data.model.AiChatMessage
import com.example.data.model.AppThemeMode
import com.example.data.model.Article
import com.example.data.model.ArticleComment
import com.example.data.model.Author
import com.example.data.model.Bookmark
import com.example.data.model.Category
import com.example.data.model.PdfCategory
import com.example.data.model.PdfDocument
import com.example.data.model.ReaderPreferences
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReadingHistory
import com.example.data.model.YearArchive
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ArticleRepository
import com.example.data.repository.DashboardRepository
import com.example.data.repository.WebsiteSyncState
import com.example.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

// Home ViewModel
class HomeViewModel(
    private val repository: ArticleRepository
) : ViewModel() {

    val allArticles: StateFlow<List<Article>> = repository.getAllArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredArticles: StateFlow<List<Article>> = repository.getFeaturedArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getCategories())

    val authors: StateFlow<List<Author>> = repository.authors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getAuthors())

    val yearArchives: StateFlow<List<YearArchive>> = repository.yearArchives
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getYearArchives())

    val pdfDocuments: StateFlow<List<PdfDocument>> = repository.pdfDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getPdfDocuments())

    val syncState: StateFlow<WebsiteSyncState> = repository.syncState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WebsiteSyncState())

    val readingHistory: StateFlow<List<ReadingHistory>> = repository.getReadingHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _scrollToTop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToTop: SharedFlow<Unit> = _scrollToTop.asSharedFlow()

    fun requestScrollToTop() {
        _scrollToTop.tryEmit(Unit)
    }

    fun refreshFromWebsite() {
        viewModelScope.launch {
            repository.syncFromWebsite()
        }
    }
}

// Explore ViewModel
class ExploreViewModel(
    private val repository: ArticleRepository
) : ViewModel() {
    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getCategories())
    val authors: StateFlow<List<Author>> = repository.authors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getAuthors())
    val yearArchives: StateFlow<List<YearArchive>> = repository.yearArchives
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getYearArchives())

    val allArticles: StateFlow<List<Article>> = repository.getAllArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(0) // 0: Categories, 1: Authors, 2: Archives, 3: Popular
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }
}

// Search ViewModel
class SearchViewModel(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategorySlug = MutableStateFlow<String?>(null)
    val selectedCategorySlug: StateFlow<String?> = _selectedCategorySlug.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getCategories())

    val yearArchives: StateFlow<List<YearArchive>> = repository.yearArchives
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getYearArchives())

    val recentSearches: StateFlow<List<String>> = repository.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchResults = MutableStateFlow<List<Article>>(emptyList())
    val searchResults: StateFlow<List<Article>> = _searchResults.asStateFlow()

    val popularSuggestions = listOf(
        "ভাষা আন্দোলন", "ইঞ্চৌঘর", "শহীদ সুদেষ্ণা সিংহ", "মিংকৌ", "বিশু উৎসব", "গোকুলানন্দ গীতিস্বামী", "লোকতাক হ্রদ", "কবিতা"
    )

    init {
        performSearch()
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        performSearch()
    }

    fun setCategoryFilter(slug: String?) {
        _selectedCategorySlug.value = if (_selectedCategorySlug.value == slug) null else slug
        performSearch()
    }

    fun setYearFilter(year: Int?) {
        _selectedYear.value = if (_selectedYear.value == year) null else year
        performSearch()
    }

    fun executeSearch(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.recordSearch(query)
            }
        }
        performSearch()
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            repository.removeSearch(query)
        }
    }

    fun clearAllRecentSearches() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    private fun performSearch() {
        viewModelScope.launch {
            repository.searchArticles(_searchQuery.value, _selectedCategorySlug.value, _selectedYear.value)
                .collect { results ->
                    _searchResults.value = results
                }
        }
    }
}

// Reader ViewModel with Text-To-Speech
class ReaderViewModel(
    private val repository: ArticleRepository,
    private val preferencesRepository: UserPreferencesRepository,
    context: Context
) : ViewModel() {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _currentArticle = MutableStateFlow<Article?>(null)
    val currentArticle: StateFlow<Article?> = _currentArticle.asStateFlow()

    private val _relatedArticles = MutableStateFlow<List<Article>>(emptyList())
    val relatedArticles: StateFlow<List<Article>> = _relatedArticles.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _comments = MutableStateFlow<List<ArticleComment>>(emptyList())
    val comments: StateFlow<List<ArticleComment>> = _comments.asStateFlow()

    private val _commentStatus = MutableStateFlow<String?>(null)
    val commentStatus: StateFlow<String?> = _commentStatus.asStateFlow()

    private val _isSubmittingComment = MutableStateFlow(false)
    val isSubmittingComment: StateFlow<Boolean> = _isSubmittingComment.asStateFlow()

    val readerPreferences: StateFlow<ReaderPreferences> = preferencesRepository.readerPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderPreferences())

    // TTS playback states
    private val _isTtsPlaying = MutableStateFlow(false)
    val isTtsPlaying: StateFlow<Boolean> = _isTtsPlaying.asStateFlow()

    private val _ttsProgressText = MutableStateFlow("")
    val ttsProgressText: StateFlow<String> = _ttsProgressText.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                val bengaliLocale = Locale("bn", "BD")
                val result = tts?.setLanguage(bengaliLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isTtsPlaying.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isTtsPlaying.value = false
                        _ttsProgressText.value = "পাঠ সমাপ্ত"
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isTtsPlaying.value = false
                    }
                })
            }
        }
    }

    fun loadArticle(articleId: String) {
        viewModelScope.launch {
            val art = repository.getArticleById(articleId)
            _currentArticle.value = art
            if (art != null && art.sourceUrl.contains("ningshingche.com")) {
                _comments.value = repository.loadComments(art.sourceUrl)
            }

            if (art != null) {
                repository.isBookmarked(art.id).collect { bookmarked ->
                    _isBookmarked.value = bookmarked
                }
            }
        }

        viewModelScope.launch {
            repository.getAllArticles().collect { all ->
                val current = _currentArticle.value
                if (current != null) {
                    _relatedArticles.value = all.filter { it.id != current.id && (it.categorySlug == current.categorySlug || it.authorId == current.authorId) }.take(4)
                }
            }
        }
    }

    fun submitComment(name: String, address: String, email: String, phone: String, content: String) {
        val art = _currentArticle.value ?: return
        if (name.isBlank() || email.isBlank() || content.isBlank()) {
            _commentStatus.value = "নাঙ, ইমেইল বারো মন্তব্য আবশ্যক।"
            return
        }
        viewModelScope.launch {
            _isSubmittingComment.value = true
            _commentStatus.value = "মন্তব্য পাঠানি অর..."
            val result = repository.submitComment(art.sourceUrl, name, address, email, phone, content)
            _isSubmittingComment.value = false
            _commentStatus.value = result.getOrElse { it.message ?: "মন্তব্য পাঠানো যায়নি" }
            if (result.isSuccess) {
                _comments.value = repository.loadComments(art.sourceUrl)
            }
        }
    }

    fun toggleBookmark() {
        val art = _currentArticle.value ?: return
        viewModelScope.launch {
            repository.toggleBookmark(art.id)
            _isBookmarked.value = !_isBookmarked.value
        }
    }

    fun updateReadingProgress(scrollPos: Int, progressPercent: Float) {
        val art = _currentArticle.value ?: return
        viewModelScope.launch {
            repository.saveReadingProgress(art.id, scrollPos, progressPercent)
        }
    }

    fun updateFontSize(newSize: Float) {
        viewModelScope.launch {
            preferencesRepository.updateFontSize(newSize.coerceIn(12f, 26f))
        }
    }

    fun updateLineSpacing(newSpacing: Float) {
        viewModelScope.launch {
            preferencesRepository.updateLineSpacing(newSpacing.coerceIn(1.2f, 2.4f))
        }
    }

    fun updateThemeMode(mode: ReaderThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(mode)
        }
    }

    fun toggleTts() {
        val article = _currentArticle.value ?: return
        if (!isTtsInitialized || tts == null) return

        if (_isTtsPlaying.value) {
            tts?.stop()
            _isTtsPlaying.value = false
            _ttsProgressText.value = "স্থগিত"
        } else {
            val speechText = "${article.title}. লেখক ${article.authorName}. ${article.content}"
            _ttsProgressText.value = "অডিও পাঠ চলছে..."
            tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "ArticleTts_${article.id}")
            _isTtsPlaying.value = true
        }
    }

    fun stopTts() {
        tts?.stop()
        _isTtsPlaying.value = false
        _ttsProgressText.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}

// Bookmarks ViewModel
class BookmarksViewModel(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _searchSavedQuery = MutableStateFlow("")
    val searchSavedQuery: StateFlow<String> = _searchSavedQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    val bookmarkedArticles: StateFlow<List<Article>> = combine(
        repository.getAllBookmarks(),
        repository.getAllArticles(),
        _searchSavedQuery,
        _selectedCategoryFilter
    ) { bookmarks, allArticles, query, categoryFilter ->
        val bookmarkIds = bookmarks.map { it.articleId }.toSet()
        val filtered = allArticles.filter { it.id in bookmarkIds }
            .filter { art ->
                val matchQuery = query.isBlank() || art.title.contains(query, ignoreCase = true) || art.authorName.contains(query, ignoreCase = true)
                val matchCategory = categoryFilter == null || art.categorySlug == categoryFilter
                matchQuery && matchCategory
            }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchSavedQuery.value = query
    }

    fun setCategoryFilter(slug: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == slug) null else slug
    }

    fun removeBookmark(articleId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(articleId)
        }
    }
}

// History ViewModel
class HistoryViewModel(
    private val repository: ArticleRepository
) : ViewModel() {

    val historyItems: StateFlow<List<Pair<ReadingHistory, Article>>> = combine(
        repository.getReadingHistory(),
        repository.getAllArticles()
    ) { historyList, allArticles ->
        val articleMap = allArticles.associateBy { it.id }
        historyList.mapNotNull { history ->
            val article = articleMap[history.articleId]
            if (article != null) history to article else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

// AI Assistant ViewModel
class AiViewModel(
    private val aiAssistant: NinghsingCheAiAssistant
) : ViewModel() {

    private val _messages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                id = "welcome",
                text = "নমস্কার! আমি নিংশিং চে AI সহকারী। আমি নিংশিংচে.কম থেকে সিঙ্ক হওয়া সব প্রবন্ধ, লেখক ও বিভাগ খুঁজে উত্তর দিই। ইঞ্চৌঘর, মিংকৌ, ভাষা আন্দোলন বা কোনো লেখকের নাম জিজ্ঞাসা করুন।",
                isUser = false,
                citations = emptyList()
            )
        )
    )
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val suggestedQuestions = listOf(
        "বিষ্ণুপ্রিয়া মণিপুরি ভাষা আন্দোলনের ইতিহাস কী?",
        "মণিপুরি সমাজের ঐতিহ্যবাহী 'ইঞ্চৌঘর' কী?",
        "মণিপুরি সমাজে 'মিংকৌ' নামপ্রথা কী?",
        "বিশু উৎসব কীভাবে পালিত হয়?",
        "শহীদ সুদেষ্ণা সিংহের আত্মত্যাগ সম্পর্কে বলুন"
    )

    fun sendQuestion(question: String) {
        if (question.isBlank() || _isLoading.value) return

        val userMessage = AiChatMessage(
            id = UUID.randomUUID().toString(),
            text = question,
            isUser = true
        )

        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = aiAssistant.answerQuestion(question)
                _messages.value = _messages.value + response
            } catch (e: Exception) {
                _messages.value = _messages.value + AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "দুঃখিত, তথ্য সংগ্রহে একটি ত্রুটি দেখা দিয়েছে। অনুগ্রহ করে পুনরায় চেষ্টা করুন।",
                    isUser = false
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Settings ViewModel
class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val articleRepository: ArticleRepository
) : ViewModel() {

    val preferences: StateFlow<ReaderPreferences> = preferencesRepository.readerPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderPreferences())

    fun updateAppThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updateAppThemeMode(mode)
        }
    }

    fun toggleNewArticlesNotif(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateNotificationNew(enabled)
        }
    }

    fun toggleFeaturedNotif(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateNotificationFeatured(enabled)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            articleRepository.clearAllCache()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            articleRepository.clearHistory()
        }
    }
}

// PDF Archive ViewModel
class PdfArchiveViewModel(
    private val repository: ArticleRepository
) : ViewModel() {
    val categories: StateFlow<List<com.example.data.model.PdfCategory>> = repository.pdfCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getPdfCategories())

    val allPdfDocuments: StateFlow<List<com.example.data.model.PdfDocument>> = repository.pdfDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getPdfDocuments())

    private val _selectedCategoryId = MutableStateFlow("pdf-cat-all")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    val filteredPdfs: StateFlow<List<com.example.data.model.PdfDocument>> = combine(
        repository.pdfDocuments,
        _selectedCategoryId
    ) { docs, categoryId ->
        if (categoryId == "pdf-cat-all" || categoryId.isBlank()) docs
        else docs.filter { it.categorySlug == categoryId || it.category == categoryId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getPdfDocuments())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
        _isLoading.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(150)
            _isLoading.value = false
        }
    }
}

// PDF Viewer ViewModel
class PdfViewerViewModel(
    private val repository: ArticleRepository,
    private val context: Context
) : ViewModel() {
    private val _pdfDocument = MutableStateFlow<com.example.data.model.PdfDocument?>(null)
    val pdfDocument: StateFlow<com.example.data.model.PdfDocument?> = _pdfDocument.asStateFlow()

    private val _pages = MutableStateFlow<List<android.graphics.Bitmap>>(emptyList())
    val pages: StateFlow<List<android.graphics.Bitmap>> = _pages.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus: StateFlow<String?> = _downloadStatus.asStateFlow()

    private var localPdfFile: java.io.File? = null

    fun loadPdf(pdfId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val doc = repository.getPdfDocumentById(pdfId)
            _pdfDocument.value = doc
            if (doc != null) {
                try {
                    val file = com.example.util.PdfHelper.getOrGeneratePdfFile(context, doc)
                    localPdfFile = file
                    val bitmaps = com.example.util.PdfHelper.renderPdfPages(file)
                    _pages.value = bitmaps
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _isLoading.value = false
        }
    }

    fun setPage(pageIndex: Int) {
        if (pageIndex in 0 until (_pages.value.size)) {
            _currentPage.value = pageIndex
        }
    }

    fun downloadPdf() {
        val doc = _pdfDocument.value ?: return
        viewModelScope.launch {
            _downloadStatus.value = "ডাউনলোড হচ্ছে..."
            val result = com.example.util.PdfHelper.savePdfToDownloads(context, doc)
            _downloadStatus.value = result.getOrElse { "ডাউনলোড ব্যর্থ হয়েছে: ${it.message}" }
        }
    }

    fun sharePdf() {
        val doc = _pdfDocument.value ?: return
        val file = localPdfFile ?: return
        com.example.util.PdfHelper.sharePdfFile(context, doc, file)
    }

    fun clearStatus() {
        _downloadStatus.value = null
    }
}

// Factory
class ViewModelFactory(
    private val repository: ArticleRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val aiAssistant: NinghsingCheAiAssistant,
    private val dashboardRepository: DashboardRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repository) as T
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> ExploreViewModel(repository) as T
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(repository) as T
            modelClass.isAssignableFrom(ReaderViewModel::class.java) -> ReaderViewModel(repository, preferencesRepository, context) as T
            modelClass.isAssignableFrom(BookmarksViewModel::class.java) -> BookmarksViewModel(repository) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(repository) as T
            modelClass.isAssignableFrom(AiViewModel::class.java) -> AiViewModel(aiAssistant) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(preferencesRepository, repository) as T
            modelClass.isAssignableFrom(PdfArchiveViewModel::class.java) -> PdfArchiveViewModel(repository) as T
            modelClass.isAssignableFrom(PdfViewerViewModel::class.java) -> PdfViewerViewModel(repository, context) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(dashboardRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
