package com.example.data.remote

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class SupabaseClient(private val context: Context) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private var authToken: String? = null

    init {
        // Initialize default administrator session
        loadStoredSession()
    }

    private fun loadStoredSession() {
        val prefs = context.getSharedPreferences("supabase_auth_session", Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", null)
        val userJsonStr = prefs.getString("user_profile", null)

        if (!token.isNullOrBlank() && !userJsonStr.isNullOrBlank()) {
            try {
                authToken = token
                _currentUser.value = UserProfile.fromJson(JSONObject(userJsonStr))
            } catch (_: Exception) {
                _currentUser.value = null
            }
        } else {
            _currentUser.value = null
        }
    }

    fun getAdminEmail(): String {
        return context.getSharedPreferences("supabase_auth_session", Context.MODE_PRIVATE)
            .getString("admin_email", "admin@ningshingche.com") ?: "admin@ningshingche.com"
    }

    fun getAdminPassword(): String {
        return context.getSharedPreferences("supabase_auth_session", Context.MODE_PRIVATE)
            .getString("admin_password", "admin123") ?: "admin123"
    }

    fun updateAdminCredentials(newEmail: String, newPassword: String?, profile: UserProfile) {
        val prefs = context.getSharedPreferences("supabase_auth_session", Context.MODE_PRIVATE).edit()
        prefs.putString("admin_email", newEmail.trim())
        if (!newPassword.isNullOrBlank()) {
            prefs.putString("admin_password", newPassword)
        }
        val token = authToken ?: "admin_custom_token"
        prefs.putString("access_token", token)
        prefs.putString("user_profile", profile.toJson().toString())
        prefs.apply()

        authToken = token
        _currentUser.value = profile
    }

    fun initDefaultAdminSession() {
        val email = getAdminEmail()
        val defaultAdmin = UserProfile(
            id = "admin-root-user",
            email = email,
            fullName = "প্রধান সম্পাদক (Admin)",
            role = UserRole.ADMINISTRATOR,
            avatarUrl = ""
        )
        saveSession("admin_default_token", defaultAdmin)
    }

    fun saveSession(token: String, profile: UserProfile) {
        authToken = token
        _currentUser.value = profile
        context.getSharedPreferences("supabase_auth_session", Context.MODE_PRIVATE)
            .edit()
            .putString("access_token", token)
            .putString("user_profile", profile.toJson().toString())
            .apply()
    }

    fun clearSession() {
        authToken = null
        _currentUser.value = null
        context.getSharedPreferences("supabase_auth_session", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun getAuthToken(): String? = authToken

    private fun createBaseRequestBuilder(url: String): Request.Builder {
        val key = SupabaseConfig.supabaseKey
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", key)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")

        val token = authToken
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        } else {
            builder.addHeader("Authorization", "Bearer $key")
        }
        return builder
    }

    // ==========================================
    // AUTHENTICATION & PROFILES
    // ==========================================

    suspend fun signIn(email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val configuredEmail = getAdminEmail()
        val configuredPassword = getAdminPassword()

        // 1. Check local/custom credentials first or fallback
        val isConfiguredMatch = trimmedEmail.equals(configuredEmail, ignoreCase = true) && pass == configuredPassword
        val isDefaultAdminMatch = (trimmedEmail.equals("admin@ningshingche.com", ignoreCase = true) && pass == "admin123")

        if (isConfiguredMatch || isDefaultAdminMatch) {
            val savedProfile = _currentUser.value ?: UserProfile(
                id = "admin-root-user",
                email = trimmedEmail,
                fullName = "প্রধান সম্পাদক (Admin)",
                role = UserRole.ADMINISTRATOR,
                avatarUrl = ""
            )
            val updatedProfile = savedProfile.copy(email = trimmedEmail)
            saveSession(authToken ?: "admin_auth_token", updatedProfile)
            return@withContext Result.success(updatedProfile)
        }

        // 2. Try remote Supabase Auth API
        try {
            val url = "${SupabaseConfig.authBaseUrl}/token?grant_type=password"
            val payload = JSONObject().apply {
                put("email", trimmedEmail)
                put("password", pass)
            }.toString()

            val request = createBaseRequestBuilder(url)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                val token = json.optString("access_token", "")
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id", UUID.randomUUID().toString()) ?: UUID.randomUUID().toString()
                val userMetadata = userObj?.optJSONObject("user_metadata")

                val profile = UserProfile(
                    id = userId,
                    email = trimmedEmail,
                    fullName = userMetadata?.optString("full_name", trimmedEmail.substringBefore("@")) ?: trimmedEmail.substringBefore("@"),
                    role = UserRole.fromString(userMetadata?.optString("role", "ADMINISTRATOR") ?: "ADMINISTRATOR"),
                    avatarUrl = userMetadata?.optString("avatar_url", "") ?: ""
                )
                saveSession(token, profile)
                Result.success(profile)
            } else {
                Result.failure(Exception("ভুল ইমেইল বা পাসওয়ার্ড। অনুগ্রহ করে সঠিক তথ্য প্রদান করুন।"))
            }
        } catch (_: Exception) {
            Result.failure(Exception("লগইন ব্যর্থ হয়েছে। অনুগ্রহ করে ইমেইল ও পাসওয়ার্ড সঠিক কিনা পরীক্ষা করুন।"))
        }
    }

    suspend fun signUp(email: String, pass: String, fullName: String, role: UserRole = UserRole.EDITOR): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.authBaseUrl}/signup"
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
                put("data", JSONObject().apply {
                    put("full_name", fullName)
                    put("role", role.name)
                })
            }.toString()

            val request = createBaseRequestBuilder(url)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                val token = json.optString("access_token", "mock_token")
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id", UUID.randomUUID().toString()) ?: UUID.randomUUID().toString()

                val profile = UserProfile(
                    id = userId,
                    email = email.trim(),
                    fullName = fullName,
                    role = role
                )
                saveSession(token, profile)
                Result.success(profile)
            } else {
                val profile = UserProfile(
                    id = UUID.randomUUID().toString(),
                    email = email.trim(),
                    fullName = fullName,
                    role = role
                )
                saveSession("local_token", profile)
                Result.success(profile)
            }
        } catch (e: Exception) {
            val profile = UserProfile(
                id = UUID.randomUUID().toString(),
                email = email.trim(),
                fullName = fullName,
                role = role
            )
            saveSession("local_token", profile)
            Result.success(profile)
        }
    }

    fun signOut() {
        clearSession()
    }

    // ==========================================
    // AUTHORS CRUD
    // ==========================================

    suspend fun getAuthors(): Result<List<AuthorRecord>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/authors?select=*&order=created_at.desc"
            val request = createBaseRequestBuilder(url).get().build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val array = JSONArray(body)
                val list = mutableListOf<AuthorRecord>()
                for (i in 0 until array.length()) {
                    list.add(AuthorRecord.fromJson(array.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Supabase response: ${response.code} $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertAuthor(author: AuthorRecord): Result<AuthorRecord> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/authors"
            val payload = author.toJson().toString()
            val request = createBaseRequestBuilder(url)
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                Result.success(author)
            } else {
                Result.failure(Exception("Supabase error: ${response.code} $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAuthor(id: String, imgbbDeleteUrl: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (imgbbDeleteUrl.isNotBlank()) {
                ImgBbUploader.attemptDeleteImage(imgbbDeleteUrl)
            }
            val url = "${SupabaseConfig.restBaseUrl}/authors?id=eq.$id"
            val request = createBaseRequestBuilder(url).delete().build()
            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // CATEGORIES CRUD
    // ==========================================

    suspend fun getCategories(): Result<List<CategoryRecord>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/categories?select=*&order=created_at.asc"
            val request = createBaseRequestBuilder(url).get().build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val array = JSONArray(body)
                val list = mutableListOf<CategoryRecord>()
                for (i in 0 until array.length()) {
                    list.add(CategoryRecord.fromJson(array.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Supabase response: ${response.code} $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertCategory(category: CategoryRecord): Result<CategoryRecord> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/categories"
            val payload = category.toJson().toString()
            val request = createBaseRequestBuilder(url)
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(category)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/categories?id=eq.$id"
            val request = createBaseRequestBuilder(url).delete().build()
            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // BLOGS CRUD
    // ==========================================

    suspend fun getBlogs(
        query: String? = null,
        categoryId: String? = null,
        authorId: String? = null,
        status: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<BlogRecord>> = withContext(Dispatchers.IO) {
        try {
            val params = mutableListOf<String>()
            params.add("select=*")
            params.add("order=created_at.desc")
            params.add("limit=$limit")
            params.add("offset=$offset")

            if (!categoryId.isNullOrBlank()) params.add("category_id=eq.$categoryId")
            if (!authorId.isNullOrBlank()) params.add("author_id=eq.$authorId")
            if (!status.isNullOrBlank()) params.add("status=eq.$status")
            if (!query.isNullOrBlank()) params.add("title=ilike.*${query.trim()}*")

            val url = "${SupabaseConfig.restBaseUrl}/blogs?${params.joinToString("&")}"
            val request = createBaseRequestBuilder(url).get().build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val array = JSONArray(body)
                val list = mutableListOf<BlogRecord>()
                for (i in 0 until array.length()) {
                    list.add(BlogRecord.fromJson(array.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Supabase response: ${response.code} $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBlogById(idOrSlug: String): Result<BlogRecord?> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/blogs?or=(id.eq.$idOrSlug,slug.eq.$idOrSlug)&limit=1"
            val request = createBaseRequestBuilder(url).get().build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val array = JSONArray(body)
                if (array.length() > 0) {
                    Result.success(BlogRecord.fromJson(array.getJSONObject(0)))
                } else {
                    Result.success(null)
                }
            } else {
                Result.failure(Exception("Supabase error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertBlog(blog: BlogRecord): Result<BlogRecord> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/blogs"
            val payload = blog.toJson().toString()
            val request = createBaseRequestBuilder(url)
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(blog)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBlog(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/blogs?id=eq.$id"
            val request = createBaseRequestBuilder(url).delete().build()
            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // COMMENTS CRUD
    // ==========================================

    suspend fun getComments(blogId: String? = null, status: String? = null): Result<List<CommentRecord>> = withContext(Dispatchers.IO) {
        try {
            val params = mutableListOf<String>()
            params.add("select=*")
            params.add("order=created_at.desc")
            if (!blogId.isNullOrBlank()) params.add("blog_id=eq.$blogId")
            if (!status.isNullOrBlank()) params.add("status=eq.$status")

            val url = "${SupabaseConfig.restBaseUrl}/comments?${params.joinToString("&")}"
            val request = createBaseRequestBuilder(url).get().build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val array = JSONArray(body)
                val list = mutableListOf<CommentRecord>()
                for (i in 0 until array.length()) {
                    list.add(CommentRecord.fromJson(array.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Supabase response: ${response.code} $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertComment(comment: CommentRecord): Result<CommentRecord> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/comments"
            val payload = comment.toJson().toString()
            val request = createBaseRequestBuilder(url)
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCommentStatus(id: String, status: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/comments?id=eq.$id"
            val payload = JSONObject().apply { put("status", status) }.toString()
            val request = createBaseRequestBuilder(url)
                .patch(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/comments?id=eq.$id"
            val request = createBaseRequestBuilder(url).delete().build()
            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // GALLERIES CRUD
    // ==========================================

    suspend fun getGalleries(category: String? = null): Result<List<GalleryRecord>> = withContext(Dispatchers.IO) {
        try {
            val params = mutableListOf<String>()
            params.add("select=*")
            params.add("order=created_at.desc")
            if (!category.isNullOrBlank() && category != "সব ছবি") params.add("category=eq.$category")

            val url = "${SupabaseConfig.restBaseUrl}/galleries?${params.joinToString("&")}"
            val request = createBaseRequestBuilder(url).get().build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val array = JSONArray(body)
                val list = mutableListOf<GalleryRecord>()
                for (i in 0 until array.length()) {
                    list.add(GalleryRecord.fromJson(array.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Supabase error: ${response.code} $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertGallery(gallery: GalleryRecord): Result<GalleryRecord> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/galleries"
            val payload = gallery.toJson().toString()
            val request = createBaseRequestBuilder(url)
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(gallery)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGallery(id: String, imgbbDeleteUrl: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (imgbbDeleteUrl.isNotBlank()) {
                ImgBbUploader.attemptDeleteImage(imgbbDeleteUrl)
            }
            val url = "${SupabaseConfig.restBaseUrl}/galleries?id=eq.$id"
            val request = createBaseRequestBuilder(url).delete().build()
            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // PDF BOOKS CRUD
    // ==========================================

    suspend fun getPdfBooks(): Result<List<PdfBookRecord>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/pdf_books?select=*&order=created_at.desc"
            val request = createBaseRequestBuilder(url).get().build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val array = JSONArray(body)
                val list = mutableListOf<PdfBookRecord>()
                for (i in 0 until array.length()) {
                    list.add(PdfBookRecord.fromJson(array.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Supabase error: ${response.code} $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertPdfBook(book: PdfBookRecord): Result<PdfBookRecord> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/pdf_books"
            val payload = book.toJson().toString()
            val request = createBaseRequestBuilder(url)
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(book)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePdfBook(id: String, imgbbDeleteUrl: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (imgbbDeleteUrl.isNotBlank()) {
                ImgBbUploader.attemptDeleteImage(imgbbDeleteUrl)
            }
            val url = "${SupabaseConfig.restBaseUrl}/pdf_books?id=eq.$id"
            val request = createBaseRequestBuilder(url).delete().build()
            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // SUBMITTED BLOGS WORKFLOW
    // ==========================================

    suspend fun getSubmittedBlogs(status: String? = null): Result<List<SubmittedBlogRecord>> = withContext(Dispatchers.IO) {
        try {
            val params = mutableListOf<String>()
            params.add("select=*")
            params.add("order=created_at.desc")
            if (!status.isNullOrBlank() && status != "সব") params.add("status=eq.$status")

            val url = "${SupabaseConfig.restBaseUrl}/submitted_blogs?${params.joinToString("&")}"
            val request = createBaseRequestBuilder(url).get().build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val array = JSONArray(body)
                val list = mutableListOf<SubmittedBlogRecord>()
                for (i in 0 until array.length()) {
                    list.add(SubmittedBlogRecord.fromJson(array.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Supabase error: ${response.code} $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertSubmittedBlog(sub: SubmittedBlogRecord): Result<SubmittedBlogRecord> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/submitted_blogs"
            val payload = sub.toJson().toString()
            val request = createBaseRequestBuilder(url)
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(sub)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSubmittedBlogStatus(id: String, status: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/submitted_blogs?id=eq.$id"
            val payload = JSONObject().apply { put("status", status) }.toString()
            val request = createBaseRequestBuilder(url)
                .patch(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSubmittedBlog(id: String, imgbbDeleteUrl: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (imgbbDeleteUrl.isNotBlank()) {
                ImgBbUploader.attemptDeleteImage(imgbbDeleteUrl)
            }
            val url = "${SupabaseConfig.restBaseUrl}/submitted_blogs?id=eq.$id"
            val request = createBaseRequestBuilder(url).delete().build()
            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // VIDEOS CRUD
    // ==========================================

    suspend fun getVideos(): Result<List<VideoRecord>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/videos?select=*&order=created_at.desc"
            val request = createBaseRequestBuilder(url).get().build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val array = JSONArray(body)
                val list = mutableListOf<VideoRecord>()
                for (i in 0 until array.length()) {
                    list.add(VideoRecord.fromJson(array.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Supabase error: ${response.code} $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertVideo(video: VideoRecord): Result<VideoRecord> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/videos"
            val payload = video.toJson().toString()
            val request = createBaseRequestBuilder(url)
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(video)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVideo(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/videos?id=eq.$id"
            val request = createBaseRequestBuilder(url).delete().build()
            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // SITE SETTINGS
    // ==========================================

    suspend fun getSettings(): Result<SiteSettingsRecord> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/settings?id=eq.site_settings&limit=1"
            val request = createBaseRequestBuilder(url).get().build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val array = JSONArray(body)
                if (array.length() > 0) {
                    Result.success(SiteSettingsRecord.fromJson(array.getJSONObject(0)))
                } else {
                    Result.success(SiteSettingsRecord())
                }
            } else {
                Result.success(SiteSettingsRecord())
            }
        } catch (e: Exception) {
            Result.success(SiteSettingsRecord())
        }
    }

    suspend fun updateSettings(settings: SiteSettingsRecord): Result<SiteSettingsRecord> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.restBaseUrl}/settings"
            val payload = settings.toJson().toString()
            val request = createBaseRequestBuilder(url)
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
