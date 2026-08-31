package com.example.data.portal

import com.example.BuildConfig

/**
 * Builds the singleton [PortalRepository].
 *
 * Called once from `NinghsingCheApp.onCreate()`. Keeping construction here means
 * the rest of the app only ever sees the repository interface, and swapping in a
 * fake (for previews or tests) is a one-line change.
 */
object PortalProvider {

    @Volatile
    private var repository: PortalRepository? = null

    fun repository(): PortalRepository = repository ?: synchronized(this) {
        repository ?: create().also { repository = it }
    }

    /** Forces the next [repository] call to rebuild — used by Settings → refresh. */
    fun invalidate() {
        synchronized(this) { repository = null }
    }

    private fun create(): PortalRepository {
        val client = PortalConfig.okHttpClient(debug = BuildConfig.DEBUG)
        val api = PortalConfig.retrofit(client).create(PortalApi::class.java)
        return PortalRepository(api)
    }
}
