package com.example.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ShimmerBox
import com.example.ui.dashboard.components.DashboardSection
import com.example.ui.dashboard.components.DashboardSidebarContent
import com.example.ui.dashboard.views.AuthorsManagementView
import com.example.ui.dashboard.views.BlogsManagementView
import com.example.ui.dashboard.views.CategoriesManagementView
import com.example.ui.dashboard.views.CommentsManagementView
import com.example.ui.dashboard.views.DashboardHomeView
import com.example.ui.dashboard.views.DashboardLoginView
import com.example.ui.dashboard.views.DashboardSettingsView
import com.example.ui.dashboard.views.GalleriesManagementView
import com.example.ui.dashboard.views.PdfBooksManagementView
import com.example.ui.dashboard.views.SubmittedBlogsManagementView
import com.example.ui.dashboard.views.UserProfileAuthDialog
import com.example.ui.dashboard.views.VideosManagementView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToReaderView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()

    // If user is not authenticated in CMS Dashboard, present the Login Form
    if (currentUser == null) {
        DashboardLoginView(
            onLoginSuccess = { loggedInUser ->
                viewModel.updateCurrentUser(loggedInUser)
            },
            onNavigateToReaderView = onNavigateToReaderView,
            onPerformSignIn = { email, password ->
                viewModel.signIn(email, password)
            },
            modifier = modifier
        )
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val currentSection by viewModel.currentSection.collectAsState()
    val isInitialSkeletonLoading by viewModel.isInitialSkeletonLoading.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val authors by viewModel.authors.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val blogs by viewModel.blogs.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val galleries by viewModel.galleries.collectAsState()
    val pdfBooks by viewModel.pdfBooks.collectAsState()
    val submittedBlogs by viewModel.submittedBlogs.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val summaryStats by viewModel.summaryStats.collectAsState()
    val recentActivities by viewModel.recentActivities.collectAsState()

    var showProfileModal by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(280.dp)
            ) {
                DashboardSidebarContent(
                    currentSection = currentSection,
                    onSelectSection = { section ->
                        viewModel.setSection(section)
                        coroutineScope.launch { drawerState.close() }
                    },
                    currentUser = currentUser,
                    onUserIconClick = {
                        showProfileModal = true
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentSection.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "নিংশিং চে ডিজিটাল আর্কাইভ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("dashboard_nav_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "মেনু খুলুন",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        // Quick Reader View button
                        FilledTonalButton(
                            onClick = onNavigateToReaderView,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .padding(end = 4.dp)
                                .testTag("dashboard_btn_to_reader_view")
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পাঠক ভিউ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Refresh Data button
                        IconButton(
                            onClick = { viewModel.refreshData() },
                            modifier = Modifier.testTag("dashboard_btn_refresh_data")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "রিফ্রেশ করুন", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Profile Avatar Button
                        IconButton(
                            onClick = { showProfileModal = true },
                            modifier = Modifier.testTag("dashboard_btn_user_profile")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "প্রোফাইল",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = modifier
        ) { innerPadding ->
            // PullToRefreshBox enables "push to down (swipe down) app will reload"
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { viewModel.refreshData() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isInitialSkeletonLoading) {
                    DashboardSkeletonView()
                } else {
                    AnimatedContent(
                        targetState = currentSection,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "DashboardSectionAnimation"
                    ) { targetSection ->
                        when (targetSection) {
                            DashboardSection.HOME -> DashboardHomeView(
                                stats = summaryStats,
                                recentActivities = recentActivities,
                                currentUser = currentUser,
                                onNavigateSection = { viewModel.setSection(it) },
                                onQuickAddBlog = { viewModel.setSection(DashboardSection.BLOGS) },
                                onQuickAddAuthor = { viewModel.setSection(DashboardSection.AUTHORS) },
                                onQuickAddPdf = { viewModel.setSection(DashboardSection.PDF_BOOKS) },
                                onQuickAddGallery = { viewModel.setSection(DashboardSection.GALLERIES) }
                            )

                            DashboardSection.AUTHORS -> AuthorsManagementView(
                                authors = authors,
                                onSaveAuthor = { viewModel.saveAuthor(it) },
                                onDeleteAuthor = { viewModel.deleteAuthor(it) }
                            )

                            DashboardSection.CATEGORIES -> CategoriesManagementView(
                                categories = categories,
                                onSaveCategory = { viewModel.saveCategory(it) },
                                onDeleteCategory = { viewModel.deleteCategory(it) }
                            )

                            DashboardSection.BLOGS -> BlogsManagementView(
                                blogs = blogs,
                                categories = categories,
                                authors = authors,
                                onSaveBlog = { viewModel.saveBlog(it) },
                                onDeleteBlog = { viewModel.deleteBlog(it) }
                            )

                            DashboardSection.COMMENTS -> CommentsManagementView(
                                comments = comments,
                                onToggleStatus = { viewModel.toggleCommentStatus(it) },
                                onDeleteComment = { viewModel.deleteComment(it) }
                            )

                            DashboardSection.GALLERIES -> GalleriesManagementView(
                                galleries = galleries,
                                onSaveGallery = { viewModel.saveGallery(it) },
                                onDeleteGallery = { viewModel.deleteGallery(it) }
                            )

                            DashboardSection.PDF_BOOKS -> PdfBooksManagementView(
                                pdfBooks = pdfBooks,
                                onSavePdfBook = { viewModel.savePdfBook(it) },
                                onDeletePdfBook = { viewModel.deletePdfBook(it) }
                            )

                            DashboardSection.SUBMIT_BLOGS -> SubmittedBlogsManagementView(
                                submissions = submittedBlogs,
                                onApproveAndPublish = { viewModel.approveAndPublishSubmission(it) },
                                onReject = { viewModel.rejectSubmission(it) },
                                onDeleteSubmission = { viewModel.deleteSubmission(it) }
                            )

                            DashboardSection.VIDEOS -> VideosManagementView(
                                videos = videos,
                                onSaveVideo = { viewModel.saveVideo(it) },
                                onDeleteVideo = { viewModel.deleteVideo(it) }
                            )

                            DashboardSection.SETTINGS -> DashboardSettingsView(
                                settings = settings,
                                onSaveSettings = { viewModel.saveSettings(it) },
                                currentUser = currentUser,
                                onUpdateUserCredentials = { newEmail, newPassword, profile ->
                                    viewModel.updateAdminCredentials(newEmail, newPassword, profile)
                                },
                                initialAdminEmail = viewModel.getAdminEmail()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProfileModal) {
        UserProfileAuthDialog(
            currentUser = currentUser,
            supabaseClient = viewModel.dashboardRepository.supabaseClient,
            onDismiss = { showProfileModal = false },
            onUserUpdated = { updated ->
                viewModel.updateCurrentUser(updated)
            }
        )
    }
}

@Composable
private fun DashboardSkeletonView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(modifier = Modifier.weight(1f).height(90.dp), shape = RoundedCornerShape(14.dp))
            ShimmerBox(modifier = Modifier.weight(1f).height(90.dp), shape = RoundedCornerShape(14.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(modifier = Modifier.weight(1f).height(90.dp), shape = RoundedCornerShape(14.dp))
            ShimmerBox(modifier = Modifier.weight(1f).height(90.dp), shape = RoundedCornerShape(14.dp))
        }
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp), shape = RoundedCornerShape(16.dp))
    }
}
