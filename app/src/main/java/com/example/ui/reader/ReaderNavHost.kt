package com.example.ui.reader

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.NinghsingCheApp
import com.example.ui.components.PortalDrawerContent
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthorsDirectoryScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeaturedScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PdfArchiveScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SocialActivitiesScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.BookmarksViewModel
import com.example.ui.viewmodel.ExploreViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.PdfArchiveViewModel
import com.example.ui.viewmodel.PdfViewerViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import java.net.URLEncoder

/**
 * Navigation routes for NingshingChe Portal.
 */
object ReaderRoute {
    const val Splash = "splash"
    const val Home = "home"
    const val Search = "search"
    const val Article = "article/{articleId}"
    const val Category = "category/{categorySlug}"
    const val Author = "author/{authorId}"
    const val AiAssistant = "ai_assistant"
    const val Settings = "settings"
    const val Bookmarks = "bookmarks"
    const val History = "history"
    const val PdfArchive = "pdf_archive"
    const val PdfViewer = "pdf_viewer/{pdfId}"
    const val Explore = "explore"
    const val Featured = "featured"
    const val About = "about"
    const val AuthorsDirectory = "authors_directory"
    const val SocialActivities = "social_activities"
    const val Dashboard = "dashboard"

    fun article(idOrSlug: String) = "article/${encode(idOrSlug)}"
    fun category(slug: String) = "category/${encode(slug)}"
    fun author(id: String) = "author/${encode(id)}"
    fun pdfViewer(pdfId: String) = "pdf_viewer/${encode(pdfId)}"

    private fun encode(value: String) =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

@Composable
fun EditorialReaderApp(
    app: NinghsingCheApp,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val portalFactory = ReaderViewModelFactory(app.portalRepository)
    val mainFactory = ViewModelFactory(
        repository = app.articleRepository,
        preferencesRepository = app.preferencesRepository,
        aiAssistant = app.aiAssistant,
        dashboardRepository = app.dashboardRepository,
        context = context
    )

    val openExternal: (String) -> Unit = { url ->
        if (url.isNotBlank()) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ReaderRoute.Home

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute == ReaderRoute.Home,
        drawerContent = {
            PortalDrawerContent(
                currentRoute = currentRoute,
                isDark = isDark,
                onNavigate = { route ->
                    coroutineScope.launch {
                        drawerState.close()
                        navController.navigate(route) {
                            popUpTo(ReaderRoute.Home) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onCategory = { slug ->
                    coroutineScope.launch {
                        drawerState.close()
                        navController.navigate(ReaderRoute.category(slug))
                    }
                },
                onYear = { year ->
                    coroutineScope.launch {
                        drawerState.close()
                        navController.navigate(ReaderRoute.category(year.toString()))
                    }
                },
                onExternal = { url ->
                    coroutineScope.launch {
                        drawerState.close()
                        openExternal(url)
                    }
                },
                onToggleTheme = onToggleTheme,
                onShareApp = {
                    coroutineScope.launch {
                        drawerState.close()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "নিংশিং চে — বিষ্ণুপ্রিয়া মণিপুরি সাহিত্য ও সংস্কৃতি পোর্টাল\nhttps://ningshingche.com"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "নিংশিং চে অ্যাপ শেয়ার করুন"))
                    }
                },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = ReaderRoute.Splash,
            modifier = modifier
        ) {
            // Splash Screen
            composable(ReaderRoute.Splash) {
                SplashScreen(
                    onSplashComplete = {
                        navController.navigate(ReaderRoute.Home) {
                            popUpTo(ReaderRoute.Splash) { inclusive = true }
                        }
                    }
                )
            }

            // Home Front Page
            composable(ReaderRoute.Home) {
                val homeViewModel: HomeViewModel = viewModel(factory = portalFactory)
                HomeScreen(
                    viewModel = homeViewModel,
                    onArticleClick = { navController.navigate(ReaderRoute.article(it)) },
                    onCategoryClick = { navController.navigate(ReaderRoute.category(it.slug)) },
                    onAuthorClick = { navController.navigate(ReaderRoute.author(it.id)) },
                    onSearchClick = { navController.navigate(ReaderRoute.Search) },
                    onPdfClick = { book -> navController.navigate(ReaderRoute.pdfViewer(book.id)) },
                    onSeeAllLatest = { navController.navigate(ReaderRoute.Search) },
                    onSeeAllFeatured = { navController.navigate(ReaderRoute.Featured) },
                    onMenuClick = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    onAiClick = {
                        navController.navigate(ReaderRoute.AiAssistant)
                    },
                    onToggleTheme = onToggleTheme,
                    onNavigate = { route -> navController.navigate(route) },
                    onOpenLink = openExternal,
                    isDark = isDark
                )
            }

            // Search Screen
            composable(ReaderRoute.Search) {
                val searchViewModel: SearchViewModel = viewModel(factory = portalFactory)
                SearchScreen(
                    viewModel = searchViewModel,
                    onBackClick = { navController.popBackStack() },
                    onArticleClick = { navController.navigate(ReaderRoute.article(it)) },
                    onCategoryClick = { navController.navigate(ReaderRoute.category(it.slug)) }
                )
            }

            // Article Detail Screen
            composable(
                route = ReaderRoute.Article,
                arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "https://ningshingche.com/article/{articleId}" },
                    navDeepLink { uriPattern = "http://ningshingche.com/article/{articleId}" },
                    navDeepLink { uriPattern = "https://ningshingche.com/{articleId}" },
                    navDeepLink { uriPattern = "http://ningshingche.com/{articleId}" }
                )
            ) { entry ->
                val articleId = entry.arguments?.getString("articleId").orEmpty()
                val articleViewModel: ArticleViewModel = viewModel(factory = portalFactory)
                LaunchedEffect(articleId) { articleViewModel.load(articleId) }
                ArticleScreen(
                    viewModel = articleViewModel,
                    onBackClick = { navController.popBackStack() },
                    onRelatedClick = { navController.navigate(ReaderRoute.article(it)) },
                    onCategoryClick = { categorySlug ->
                        if (categorySlug.isNotBlank()) {
                            navController.navigate(ReaderRoute.category(categorySlug))
                        }
                    },
                    onAuthorClick = { authorId ->
                        if (authorId.isNotBlank()) {
                            navController.navigate(ReaderRoute.author(authorId))
                        }
                    },
                    onTagClick = { tag ->
                        if (tag.isNotBlank()) {
                            navController.navigate(ReaderRoute.category(tag))
                        }
                    }
                )
            }

            // Category Articles Screen
            composable(
                route = ReaderRoute.Category,
                arguments = listOf(navArgument("categorySlug") { type = NavType.StringType })
            ) { entry ->
                val slug = entry.arguments?.getString("categorySlug").orEmpty()
                val categoryViewModel: CategoryViewModel = viewModel(
                    factory = CategoryViewModelFactory(app.portalRepository, slug)
                )
                CategoryScreen(
                    viewModel = categoryViewModel,
                    onBackClick = { navController.popBackStack() },
                    onArticleClick = { navController.navigate(ReaderRoute.article(it)) }
                )
            }

            // Author Profile Screen
            composable(
                route = ReaderRoute.Author,
                arguments = listOf(navArgument("authorId") { type = NavType.StringType })
            ) { entry ->
                val authorId = entry.arguments?.getString("authorId").orEmpty()
                val authorViewModel: AuthorViewModel = viewModel(
                    factory = AuthorViewModelFactory(app.portalRepository, authorId)
                )
                AuthorScreen(
                    viewModel = authorViewModel,
                    onBackClick = { navController.popBackStack() },
                    onArticleClick = { navController.navigate(ReaderRoute.article(it)) }
                )
            }

            // NingshingChe AI Assistant Screen
            composable(ReaderRoute.AiAssistant) {
                val aiViewModel: AiViewModel = viewModel(factory = mainFactory)
                AiAssistantScreen(
                    viewModel = aiViewModel,
                    onBackClick = { navController.popBackStack() },
                    onArticleClick = { navController.navigate(ReaderRoute.article(it)) },
                    onMenuClick = {
                        coroutineScope.launch { drawerState.open() }
                    }
                )
            }

            // Settings Screen
            composable(ReaderRoute.Settings) {
                val settingsViewModel: SettingsViewModel = viewModel(factory = mainFactory)
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Bookmarks / Saved Screen
            composable(ReaderRoute.Bookmarks) {
                val bookmarksViewModel: BookmarksViewModel = viewModel(factory = mainFactory)
                BookmarksScreen(
                    viewModel = bookmarksViewModel,
                    onArticleClick = { navController.navigate(ReaderRoute.article(it)) }
                )
            }

            // Reading History Screen
            composable(ReaderRoute.History) {
                val historyViewModel: HistoryViewModel = viewModel(factory = mainFactory)
                HistoryScreen(
                    viewModel = historyViewModel,
                    onBackClick = { navController.popBackStack() },
                    onArticleClick = { navController.navigate(ReaderRoute.article(it)) }
                )
            }

            // PDF Archive Screen
            composable(ReaderRoute.PdfArchive) {
                val pdfViewModel: PdfArchiveViewModel = viewModel(factory = mainFactory)
                PdfArchiveScreen(
                    viewModel = pdfViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenPdf = { pdfId ->
                        navController.navigate(ReaderRoute.pdfViewer(pdfId))
                    }
                )
            }

            // PDF Viewer Screen
            composable(
                route = ReaderRoute.PdfViewer,
                arguments = listOf(navArgument("pdfId") { type = NavType.StringType })
            ) { entry ->
                val pdfId = entry.arguments?.getString("pdfId").orEmpty()
                val pdfViewerViewModel: PdfViewerViewModel = viewModel(factory = mainFactory)
                PdfViewerScreen(
                    pdfId = pdfId,
                    viewModel = pdfViewerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Explore Categories & Authors Screen
            composable(ReaderRoute.Explore) {
                val exploreViewModel: ExploreViewModel = viewModel(factory = mainFactory)
                ExploreScreen(
                    viewModel = exploreViewModel,
                    onArticleClick = { navController.navigate(ReaderRoute.article(it)) },
                    onCategoryClick = { navController.navigate(ReaderRoute.category(it)) },
                    onAuthorClick = { navController.navigate(ReaderRoute.author(it)) },
                    onArchiveClick = { navController.navigate(ReaderRoute.category(it.toString())) }
                )
            }

            // Featured Articles Screen
            composable(ReaderRoute.Featured) {
                val homeViewModel: com.example.ui.viewmodel.HomeViewModel = viewModel(factory = mainFactory)
                FeaturedScreen(
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() },
                    onArticleClick = { navController.navigate(ReaderRoute.article(it)) }
                )
            }

            // About Screen
            composable(ReaderRoute.About) {
                AboutScreen()
            }

            // Authors Directory Screen
            composable(ReaderRoute.AuthorsDirectory) {
                val exploreViewModel: ExploreViewModel = viewModel(factory = mainFactory)
                AuthorsDirectoryScreen(
                    viewModel = exploreViewModel,
                    onAuthorClick = { navController.navigate(ReaderRoute.author(it)) }
                )
            }

            // Social Activities Screen
            composable(ReaderRoute.SocialActivities) {
                val exploreViewModel: ExploreViewModel = viewModel(factory = mainFactory)
                SocialActivitiesScreen(
                    viewModel = exploreViewModel,
                    onArticleClick = { navController.navigate(ReaderRoute.article(it)) }
                )
            }

            // CMS Dashboard Screen
            composable(ReaderRoute.Dashboard) {
                val dashboardViewModel: DashboardViewModel = viewModel(factory = mainFactory)
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToReaderView = { navController.popBackStack() }
                )
            }
        }
    }
}
