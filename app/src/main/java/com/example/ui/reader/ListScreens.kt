package com.example.ui.reader

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.portal.ArticleSummary
import com.example.data.portal.AuthorRef
import com.example.data.portal.CategoryRef
import com.example.ui.editorial.ArticleRow
import com.example.ui.editorial.AuthorChip
import com.example.ui.editorial.CategoryPill
import com.example.ui.editorial.EditorialShape
import com.example.ui.editorial.EditorialSpace
import com.example.ui.editorial.EditorialType
import com.example.ui.editorial.EmptyState
import com.example.ui.editorial.ErrorState
import com.example.ui.editorial.Hairline
import com.example.ui.editorial.LoadingFeed
import com.example.ui.editorial.LocalEditorialTokens
import com.example.ui.editorial.SectionHeader

/**
 * The three list screens — search, category and author — share one paging
 * contract ([ListUiState]) and one rendering routine ([ArticleList]), so a
 * change to row behaviour or pagination touches a single place.
 */

@Composable
internal fun ArticleList(
    state: ListUiState,
    onArticleClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = { },
    header: @Composable (() -> Unit)? = null
) {
    val listState = rememberLazyListState()

    // Infinite scroll: request the next page when the last rendered row is
    // within three positions of the end.
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && state is ListUiState.Ready && !state.isLoadingMore && !state.endReached) {
            onLoadMore()
        }
    }

    when (state) {
        ListUiState.Loading -> LoadingFeed(modifier = modifier)

        is ListUiState.Error -> ErrorState(
            message = state.message,
            onRetry = onRetry,
            modifier = modifier
        )

        is ListUiState.Ready -> {
            if (state.articles.isEmpty()) {
                EmptyState(message = "এখানে কোনো প্রবন্ধ পাওয়া যায়নি।", modifier = modifier)
                return
            }
            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = EditorialSpace.xxl)
            ) {
                if (header != null) item { header() }
                items(state.articles, key = { it.id }) { article ->
                    ArticleRow(article = article, onClick = { onArticleClick(article.id) })
                    Hairline(modifier = Modifier.padding(horizontal = EditorialSpace.gutter))
                }
                item {
                    when {
                        state.isLoadingMore -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(EditorialSpace.lg),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = LocalEditorialTokens.current.accent
                            )
                        }
                        state.endReached && state.articles.isNotEmpty() -> Text(
                            text = "— শেষ —",
                            style = EditorialType.Caption,
                            color = LocalEditorialTokens.current.inkMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(EditorialSpace.lg),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        else -> Spacer(Modifier.height(EditorialSpace.md))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBackClick: () -> Unit,
    onArticleClick: (String) -> Unit,
    onCategoryClick: (CategoryRef) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val query by viewModel.query.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var field by remember { mutableStateOf(TextFieldValue(query)) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("অনুসন্ধান", style = EditorialType.Title) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পেছনে")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                SearchField(
                    value = field,
                    onValueChange = {
                        field = it
                        viewModel.onQueryChange(it.text)
                    },
                    onSubmit = { viewModel.submit(field.text) },
                    modifier = Modifier.padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.xs)
                )
                if (categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.sm)
                    ) {
                        // Horizontal pager would be overkill; a wrapped row of
                        // chips keeps all fifteen categories reachable.
                        Column(verticalArrangement = Arrangement.spacedBy(EditorialSpace.xs)) {
                            categories.chunked(4).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(EditorialSpace.xs)) {
                                    row.forEach { category ->
                                        CategoryPill(title = category.title, onClick = { onCategoryClick(category) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (query.trim().length < 2) {
            EmptyState(
                message = "অন্তত দুই অক্ষর লিখুন। শিরোনাম, উপশিরোনাম ও স্লাগে খোঁজা হয়।",
                modifier = Modifier.padding(padding)
            )
        } else {
            ArticleList(
                state = state,
                onArticleClick = onArticleClick,
                onLoadMore = viewModel::loadMore,
                onRetry = { viewModel.submit(field.text) },
                modifier = Modifier.padding(padding)
            ) {
                if (state is ListUiState.Ready) {
                    val total = (state as ListUiState.Ready).total
                    SectionHeader(
                        title = "ফলাফল",
                        subtitle = if (total != null) "$total টি প্রবন্ধ" else null
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalEditorialTokens.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = EditorialType.Body,
        placeholder = { Text("প্রবন্ধ খুঁজুন...", style = EditorialType.Body, color = tokens.inkMuted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = tokens.inkMuted) },
        trailingIcon = {
            if (value.text.isNotEmpty()) {
                IconButton(onClick = { onValueChange(TextFieldValue("")); onSubmit() }) {
                    Icon(Icons.Default.Close, contentDescription = "মুছুন", tint = tokens.inkMuted)
                }
            }
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(EditorialShape.chip),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ---------------------------------------------------------------------------
// Category
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel,
    onBackClick: () -> Unit,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val category by viewModel.category.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(category?.title ?: "বিভাগ", style = EditorialType.Title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পেছনে")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        ArticleList(
            state = state,
            onArticleClick = onArticleClick,
            onLoadMore = viewModel::loadMore,
            onRetry = { viewModel.load() },
            modifier = Modifier.padding(padding)
        ) {
            Column(modifier = Modifier.padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.md)) {
                Text(
                    text = category?.title.orEmpty(),
                    style = EditorialType.Display,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!category?.subTitle.isNullOrBlank()) {
                    Spacer(Modifier.height(EditorialSpace.xs))
                    Text(
                        text = category?.subTitle.orEmpty(),
                        style = EditorialType.Body,
                        color = LocalEditorialTokens.current.inkSoft
                    )
                }
                if (state is ListUiState.Ready) {
                    val total = (state as ListUiState.Ready).total
                    if (total != null) {
                        Spacer(Modifier.height(EditorialSpace.sm))
                        Text(
                            text = "$total টি প্রবন্ধ",
                            style = EditorialType.Caption,
                            color = LocalEditorialTokens.current.inkMuted
                        )
                    }
                }
                Spacer(Modifier.height(EditorialSpace.sm))
                Hairline()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Author
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorScreen(
    viewModel: AuthorViewModel,
    onBackClick: () -> Unit,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val author by viewModel.author.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(author?.name ?: "লেখক", style = EditorialType.Title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পেছনে")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        ArticleList(
            state = state,
            onArticleClick = onArticleClick,
            onLoadMore = viewModel::loadMore,
            onRetry = { viewModel.load() },
            modifier = Modifier.padding(padding)
        ) {
            AuthorHeader(author = author)
        }
    }
}

@Composable
private fun AuthorHeader(author: AuthorRef?) {
    if (author == null) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(EditorialSpace.gutter),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthorChip(author = author, onClick = { })
        Spacer(Modifier.height(EditorialSpace.md))
        if (author.bio.isNotBlank()) {
            Text(
                text = author.bio.replace(Regex("<[^>]+>"), " "),
                style = EditorialType.BodySmall,
                color = LocalEditorialTokens.current.inkSoft,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (author.location.isNotBlank()) {
            Spacer(Modifier.height(EditorialSpace.xs))
            Text(
                text = author.location,
                style = EditorialType.Caption,
                color = LocalEditorialTokens.current.inkMuted
            )
        }
        Spacer(Modifier.height(EditorialSpace.md))
        Hairline()
    }
}
