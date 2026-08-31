package com.example.ui.reader

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.portal.ArticleSummary
import com.example.data.portal.AuthorRef
import com.example.data.portal.CategoryRef
import com.example.data.portal.GalleryItem
import com.example.data.portal.PdfBook
import com.example.data.portal.VideoItem
import com.example.ui.editorial.EditorialFooter
import com.example.ui.components.HomeSkeletonLayout
import com.example.ui.components.NingshingCheBrandLogo
import com.example.ui.editorial.AiAssistantHomeBanner
import com.example.ui.editorial.AnimatedHamburgerIcon
import com.example.ui.editorial.ArticleRail
import com.example.ui.editorial.ArticleRow
import com.example.ui.editorial.AuthorRail
import com.example.ui.editorial.CategoryRail
import com.example.ui.editorial.EditorialSpace
import com.example.ui.editorial.EditorialType
import com.example.ui.editorial.EmptyState
import com.example.ui.editorial.ErrorState
import com.example.ui.editorial.GalleryGrid
import com.example.ui.editorial.GalleryModalDialog
import com.example.ui.editorial.VideoPlayerDialog
import com.example.ui.editorial.Hairline
import com.example.ui.editorial.HeroArticleCard
import com.example.ui.editorial.LocalEditorialTokens
import com.example.ui.editorial.NumberedArticleCard
import com.example.ui.editorial.PdfRail
import com.example.ui.editorial.SectionHeader
import com.example.ui.editorial.VideoRail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Home — the magazine's front page.
 *
 * Structure, top to bottom:
 * masthead (Fixed top bar with animated hamburger & logo) →
 * hero carousel (auto sliding, is_slider filter) →
 * AI Assistant Banner →
 * category rail (first article thumb backdrop, Kalpurush) →
 * featured rail (is_featured filter, All Featured navigation) →
 * numbered specials →
 * gallery strip (in-app modal dialog) →
 * PDF shelf (in-app PDF reader) →
 * video rail →
 * running latest list.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onArticleClick: (String) -> Unit,
    onCategoryClick: (CategoryRef) -> Unit,
    onAuthorClick: (AuthorRef) -> Unit,
    onSearchClick: () -> Unit,
    onPdfClick: (PdfBook) -> Unit,
    onSeeAllLatest: () -> Unit,
    onSeeAllFeatured: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onAiClick: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onOpenLink: (String) -> Unit = {},
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val offlineNotice by viewModel.offlineNotice.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isRefreshing = (state as? HomeUiState.Ready)?.isRefreshing == true
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(offlineNotice) {
        offlineNotice?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // No text "Ningshing Che" as per design mandate
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier.testTag("hamburger_menu_button")
                        ) {
                            AnimatedHamburgerIcon(tint = MaterialTheme.colorScheme.onSurface)
                        }
                        NingshingCheBrandLogo(
                            size = 32.dp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                }
                                .testTag("brand_logo_home_button")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDark) "লাইট থিম চালু করুন" else "ডার্ক থিম চালু করুন",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onAiClick,
                        modifier = Modifier.testTag("ai_assistant_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI সহকারী",
                            tint = LocalEditorialTokens.current.accent
                        )
                    }
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.testTag("search_top_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "অনুসন্ধান")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.load(force = true) },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val current = state) {
                HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HomeSkeletonLayout()
                    }
                }

                is HomeUiState.Error -> ErrorState(
                    message = current.message,
                    onRetry = { viewModel.load(force = true) },
                    modifier = Modifier.fillMaxSize()
                )

                is HomeUiState.Ready -> HomeContent(
                    feed = current.feed,
                    listState = listState,
                    onArticleClick = onArticleClick,
                    onCategoryClick = onCategoryClick,
                    onAuthorClick = onAuthorClick,
                    onPdfClick = onPdfClick,
                    onSeeAllLatest = onSeeAllLatest,
                    onSeeAllFeatured = onSeeAllFeatured,
                    onAiClick = onAiClick,
                    onNavigate = onNavigate,
                    onOpenLink = onOpenLink
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    feed: com.example.data.portal.HomeFeed,
    listState: LazyListState,
    onArticleClick: (String) -> Unit,
    onCategoryClick: (CategoryRef) -> Unit,
    onAuthorClick: (AuthorRef) -> Unit,
    onPdfClick: (PdfBook) -> Unit,
    onSeeAllLatest: () -> Unit,
    onSeeAllFeatured: () -> Unit,
    onAiClick: () -> Unit,
    onNavigate: (String) -> Unit = {},
    onOpenLink: (String) -> Unit = {}
) {
    val context = LocalContext.current
    // Index (not the item) so the viewer can page through the whole gallery.
    var selectedGalleryIndex by remember { mutableStateOf<Int?>(null) }
    var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }

    // Aggregate all articles for category thumb lookup
    val allArticles = remember(feed) {
        (feed.hero + feed.featured + feed.special + feed.latest).distinctBy { it.id }
    }

    // Filter hero slider articles: prioritize isSlider == true
    val heroArticles = remember(feed.hero) {
        val sliderOnly = feed.hero.filter { it.isSlider }
        if (sliderOnly.isNotEmpty()) sliderOnly else feed.hero
    }

    // Filter featured articles: prioritize isFeature == true
    val featuredArticles = remember(feed.featured, feed.latest) {
        val featOnly = feed.featured.filter { it.isFeature }
        if (featOnly.isNotEmpty()) featOnly else feed.featured
    }

    // Interactive in-app gallery viewer - swipe left/right for the next/previous photo.
    selectedGalleryIndex?.let { index ->
        GalleryModalDialog(
            items = feed.gallery,
            initialIndex = index,
            onDismiss = { selectedGalleryIndex = null },
            onShare = { item ->
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "${item.title}\n${item.description}\n${item.imageUrl}\n\nনিংশিং চে"
                    )
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "ছবি শেয়ার করুন"))
            }
        )
    }

    // In-app video player (YouTube iframe / Facebook video plugin in a WebView).
    selectedVideo?.let { video ->
        VideoPlayerDialog(
            video = video,
            onDismiss = { selectedVideo = null },
            onOpenExternal = { url ->
                if (url.isNotBlank()) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
            }
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = EditorialSpace.xxl)
    ) {
        // 1. Hero Section Carousel (Auto Sliding)
        if (feed.settings.heroSliderEnabled && heroArticles.isNotEmpty()) {
            item {
                HeroCarousel(hero = heroArticles, onArticleClick = onArticleClick)
            }
        }

        // 2. AI Assistant Banner (Placed right after Hero Carousel)
        item {
            AiAssistantHomeBanner(
                onAiClick = onAiClick,
                modifier = Modifier.padding(top = EditorialSpace.xs, bottom = EditorialSpace.xs)
            )
        }

        // 3. Category Section (Visual Cards with first article thumbnail)
        if (feed.categories.isNotEmpty()) {
            item {
                Spacer(Modifier.height(EditorialSpace.xs))
                CategoryRail(
                    categories = feed.categories,
                    articles = allArticles,
                    selectedSlug = null,
                    onSelect = onCategoryClick
                )
                Spacer(Modifier.height(EditorialSpace.sm))
                Hairline()
            }
        }

        // 4. Featured Section (Articles with is_featured == true)
        if (feed.settings.featuredEnabled && featuredArticles.isNotEmpty()) {
            item {
                ArticleRail(
                    title = "ফিচার্ড",
                    articles = featuredArticles,
                    onArticleClick = { onArticleClick(it.id) },
                    onSeeAll = onSeeAllFeatured
                )
            }
        }

        // 5. Special Curated Section
        if (feed.special.isNotEmpty() && feed.settings.specialEnabled) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "বিশেষ নির্বাচন", subtitle = "সম্পাদকের পছন্দ")
                    feed.special.forEachIndexed { index, article ->
                        NumberedArticleCard(
                            index = index + 1,
                            article = article,
                            onClick = { onArticleClick(article.id) }
                        )
                        if (index < feed.special.lastIndex) {
                            Hairline(
                                modifier = Modifier.padding(
                                    horizontal = EditorialSpace.gutter,
                                    vertical = EditorialSpace.xxs
                                )
                            )
                        }
                    }
                }
            }
        }

        // 6. Photo Gallery (ছবি ঘর - In-App Modal Box)
        if (feed.gallery.isNotEmpty()) {
            item {
                GalleryGrid(
                    items = feed.gallery,
                    onItemClick = { item ->
                        selectedGalleryIndex = feed.gallery.indexOf(item)
                    }
                )
            }
        }

        // 7. Books & Periodicals (বই ও সাময়িকী - In-App PDF Reader)
        if (feed.pdfBooks.isNotEmpty()) {
            item {
                PdfRail(
                    books = feed.pdfBooks,
                    onBookClick = onPdfClick
                )
            }
        }

        // 8. Video Rail
        if (feed.videos.isNotEmpty()) {
            item { VideoRail(videos = feed.videos, onVideoClick = { selectedVideo = it }) }
        }

        // 9. Authors Rail
        if (feed.authors.isNotEmpty()) {
            item { AuthorRail(authors = feed.authors, onAuthorClick = onAuthorClick) }
        }

        // 10. Latest Articles Running Feed
        if (feed.latest.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "সাম্প্রতিক",
                    subtitle = feed.settings.description,
                    actionLabel = "সব",
                    onAction = onSeeAllLatest
                )
            }
            items(feed.latest, key = { it.id }) { article ->
                ArticleRow(article = article, onClick = { onArticleClick(article.id) })
                Hairline(modifier = Modifier.padding(horizontal = EditorialSpace.gutter))
            }
        } else {
            item {
                EmptyState(message = "এখনো কোনো প্রবন্ধ প্রকাশিত হয়নি।")
            }
        }

        item {
            EditorialFooter(
                settings = feed.settings,
                onNavigate = onNavigate,
                onOpenLink = onOpenLink
            )
        }
    }
}

/**
 * Auto-sliding Hero Carousel.
 * Cycles every 3.5s unless being scrolled by user.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCarousel(
    hero: List<ArticleSummary>,
    onArticleClick: (String) -> Unit
) {
    if (hero.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { hero.size })

    // Auto-slide effect
    LaunchedEffect(pagerState.pageCount, hero.size) {
        if (hero.size > 1) {
            while (true) {
                delay(3500)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % hero.size
                    pagerState.animateScrollToPage(
                        page = nextPage,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            pageSpacing = EditorialSpace.md,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            HeroArticleCard(
                article = hero[page],
                onClick = { onArticleClick(hero[page].id) }
            )
        }
        if (hero.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = EditorialSpace.sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(hero.size) { index ->
                    val selected = pagerState.currentPage == index
                    val tokens = LocalEditorialTokens.current
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (selected) 20.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) tokens.accent else tokens.ruleStrong)
                    )
                }
            }
        }
    }
}

@Composable
internal fun SeeAllButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = EditorialSpace.md, vertical = EditorialSpace.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("সব", style = EditorialType.Subtitle, color = LocalEditorialTokens.current.accent)
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = LocalEditorialTokens.current.accent
            )
        }
    }
}
