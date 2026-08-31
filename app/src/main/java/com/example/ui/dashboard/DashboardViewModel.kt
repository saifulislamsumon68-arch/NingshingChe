package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.AuthorRecord
import com.example.data.remote.BlogRecord
import com.example.data.remote.CategoryRecord
import com.example.data.remote.CommentRecord
import com.example.data.remote.DashboardSummaryStats
import com.example.data.remote.GalleryRecord
import com.example.data.remote.PdfBookRecord
import com.example.data.remote.RecentActivityItem
import com.example.data.remote.SiteSettingsRecord
import com.example.data.remote.SubmittedBlogRecord
import com.example.data.remote.UserProfile
import com.example.data.remote.VideoRecord
import com.example.data.repository.DashboardRepository
import com.example.ui.dashboard.components.DashboardSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _currentSection = MutableStateFlow(DashboardSection.HOME)
    val currentSection: StateFlow<DashboardSection> = _currentSection.asStateFlow()

    private val _isInitialSkeletonLoading = MutableStateFlow(true)
    val isInitialSkeletonLoading: StateFlow<Boolean> = _isInitialSkeletonLoading.asStateFlow()

    val authors = dashboardRepository.authors
    val categories = dashboardRepository.categories
    val blogs = dashboardRepository.blogs
    val comments = dashboardRepository.comments
    val galleries = dashboardRepository.galleries
    val pdfBooks = dashboardRepository.pdfBooks
    val submittedBlogs = dashboardRepository.submittedBlogs
    val videos = dashboardRepository.videos
    val settings = dashboardRepository.settings
    val summaryStats = dashboardRepository.summaryStats
    val recentActivities = dashboardRepository.recentActivities
    val isLoading = dashboardRepository.isLoading

    val currentUser = dashboardRepository.supabaseClient.currentUser

    init {
        viewModelScope.launch {
            // Guaranteed 1-second skeleton loading state
            delay(1000)
            _isInitialSkeletonLoading.value = false
        }
    }

    fun setSection(section: DashboardSection) {
        _currentSection.value = section
    }

    fun refreshData() {
        viewModelScope.launch {
            dashboardRepository.refreshAll()
        }
    }

    // Authors
    fun saveAuthor(author: AuthorRecord) {
        viewModelScope.launch {
            dashboardRepository.saveAuthor(author)
        }
    }

    fun deleteAuthor(id: String) {
        viewModelScope.launch {
            dashboardRepository.deleteAuthor(id)
        }
    }

    // Categories
    fun saveCategory(category: CategoryRecord) {
        viewModelScope.launch {
            dashboardRepository.saveCategory(category)
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            dashboardRepository.deleteCategory(id)
        }
    }

    // Blogs
    fun saveBlog(blog: BlogRecord) {
        viewModelScope.launch {
            dashboardRepository.saveBlog(blog)
        }
    }

    fun deleteBlog(id: String) {
        viewModelScope.launch {
            dashboardRepository.deleteBlog(id)
        }
    }

    // Comments
    fun toggleCommentStatus(id: String) {
        viewModelScope.launch {
            dashboardRepository.toggleCommentStatus(id)
        }
    }

    fun deleteComment(id: String) {
        viewModelScope.launch {
            dashboardRepository.deleteComment(id)
        }
    }

    // Galleries
    fun saveGallery(gallery: GalleryRecord) {
        viewModelScope.launch {
            dashboardRepository.saveGallery(gallery)
        }
    }

    fun deleteGallery(id: String) {
        viewModelScope.launch {
            dashboardRepository.deleteGallery(id)
        }
    }

    // PDF Books
    fun savePdfBook(book: PdfBookRecord) {
        viewModelScope.launch {
            dashboardRepository.savePdfBook(book)
        }
    }

    fun deletePdfBook(id: String) {
        viewModelScope.launch {
            dashboardRepository.deletePdfBook(id)
        }
    }

    // Submitted Blogs
    fun approveAndPublishSubmission(sub: SubmittedBlogRecord) {
        viewModelScope.launch {
            dashboardRepository.approveAndPublishSubmission(sub)
        }
    }

    fun rejectSubmission(id: String) {
        viewModelScope.launch {
            dashboardRepository.updateSubmissionStatus(id, "Rejected")
        }
    }

    fun deleteSubmission(id: String) {
        viewModelScope.launch {
            dashboardRepository.deleteSubmittedBlog(id)
        }
    }

    // Videos
    fun saveVideo(video: VideoRecord) {
        viewModelScope.launch {
            dashboardRepository.saveVideo(video)
        }
    }

    fun deleteVideo(id: String) {
        viewModelScope.launch {
            dashboardRepository.deleteVideo(id)
        }
    }

    // Settings
    fun saveSettings(settings: SiteSettingsRecord) {
        viewModelScope.launch {
            dashboardRepository.saveSettings(settings)
        }
    }

    // User Profile & Authentication
    fun getAdminEmail(): String = dashboardRepository.supabaseClient.getAdminEmail()
    fun getAdminPassword(): String = dashboardRepository.supabaseClient.getAdminPassword()

    suspend fun signIn(email: String, pass: String): Result<UserProfile> {
        return dashboardRepository.supabaseClient.signIn(email, pass)
    }

    fun updateCurrentUser(profile: UserProfile) {
        dashboardRepository.supabaseClient.saveSession(
            dashboardRepository.supabaseClient.getAuthToken() ?: "",
            profile
        )
    }

    fun updateAdminCredentials(newEmail: String, newPassword: String?, profile: UserProfile) {
        dashboardRepository.supabaseClient.updateAdminCredentials(newEmail, newPassword, profile)
    }

    fun signOut() {
        dashboardRepository.supabaseClient.clearSession()
    }
}
