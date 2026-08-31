package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.ai.NinghsingCheAiAssistant
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.portal.PortalProvider
import com.example.data.portal.PortalRepository
import com.example.data.remote.NingshingCheWebsiteClient
import com.example.data.remote.SupabaseClient
import com.example.data.repository.ArticleRepository
import com.example.data.repository.DashboardRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NinghsingCheApp : Application(), ImageLoaderFactory {

    lateinit var database: AppDatabase
        private set

    lateinit var articleRepository: ArticleRepository
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var aiAssistant: NinghsingCheAiAssistant
        private set

    lateinit var websiteClient: NingshingCheWebsiteClient
        private set

    lateinit var supabaseClient: SupabaseClient
        private set

    lateinit var dashboardRepository: DashboardRepository
        private set

    /** Live, read-only Supabase client used by the public reader UI. */
    lateinit var portalRepository: PortalRepository
        private set

    /** Shared OkHttp client used by both the Portal API and Coil image loading,
     *  so TCP connection pools, TLS handshakes and DNS caches are reused across
     *  all network calls, reducing per-request overhead significantly. */
    private var _imageOkHttpClient: OkHttpClient? = null
    private val imageOkHttpClient: OkHttpClient
        get() {
            val existing = _imageOkHttpClient
            if (existing != null) return existing
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                        .build()
                    chain.proceed(request)
                }
                .build()
                .also { _imageOkHttpClient = it }
        }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // Memory cache — 25 % of available heap, plenty for a reading app
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            // Disk cache — 250 MB on the file system; survives app restarts.
            // Using cacheDir.resolve("coil") keeps it inside the app's private
            // cache directory, which Android can reclaim when storage is low.
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250 MB
                    .build()
            }
            // Reuse the shared OkHttp client so connections and TLS sessions
            // are pooled together with the rest of the app's networking.
            .okHttpClient(imageOkHttpClient)
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Safe global exception handler to log any startup issues
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("NinghsingCheApp", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        preferencesRepository = UserPreferencesRepository(this)
        database = AppDatabase.getInstance(this)
        websiteClient = NingshingCheWebsiteClient()
        supabaseClient = SupabaseClient(this)
        dashboardRepository = DashboardRepository(this, supabaseClient, database)
        articleRepository = ArticleRepository(database, supabaseClient, websiteClient)
        portalRepository = PortalProvider.repository()
        aiAssistant = NinghsingCheAiAssistant(articleRepository, portalRepository)
    }

    companion object {
        lateinit var instance: NinghsingCheApp
            private set
    }
}