package com.example

import android.app.Application
import com.example.data.ai.NinghsingCheAiAssistant
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.portal.PortalProvider
import com.example.data.portal.PortalRepository
import com.example.data.remote.NingshingCheWebsiteClient
import com.example.data.remote.SupabaseClient
import com.example.data.repository.ArticleRepository
import com.example.data.repository.DashboardRepository

class NinghsingCheApp : Application() {

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
        aiAssistant = NinghsingCheAiAssistant(articleRepository)
        portalRepository = PortalProvider.repository()
    }

    companion object {
        lateinit var instance: NinghsingCheApp
            private set
    }
}
