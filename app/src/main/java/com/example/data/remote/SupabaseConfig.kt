package com.example.data.remote

import com.example.BuildConfig

object SupabaseConfig {
    // Configured Supabase credentials with fallback to injected BuildConfig
    const val DEFAULT_SUPABASE_URL = "https://slcpvmpsynkqdozvlsii.supabase.co"
    const val DEFAULT_PUBLISHABLE_KEY = "sb_publishable_jqJACnQHmCMcGjt0kG6Sug_ddknIbAA"
    const val DEFAULT_IMGBB_API_KEY = "576f654932a2b7398e765cf27d8c73d4"

    val supabaseUrl: String
        get() = try {
            val key = BuildConfig.SUPABASE_URL
            if (!key.isNullOrBlank() && !key.startsWith("MY_")) key else DEFAULT_SUPABASE_URL
        } catch (_: Throwable) {
            DEFAULT_SUPABASE_URL
        }

    val supabaseKey: String
        get() = try {
            val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY
            if (!key.isNullOrBlank() && !key.startsWith("MY_")) key else DEFAULT_PUBLISHABLE_KEY
        } catch (_: Throwable) {
            DEFAULT_PUBLISHABLE_KEY
        }

    val imgbbApiKey: String
        get() = try {
            val key = BuildConfig.IMGBB_API_KEY
            if (!key.isNullOrBlank() && !key.startsWith("MY_")) key else DEFAULT_IMGBB_API_KEY
        } catch (_: Throwable) {
            DEFAULT_IMGBB_API_KEY
        }

    val restBaseUrl: String
        get() = "${supabaseUrl.trimEnd('/')}/rest/v1"

    val authBaseUrl: String
        get() = "${supabaseUrl.trimEnd('/')}/auth/v1"

    val imgbbUploadUrl: String
        get() = "https://api.imgbb.com/1/upload"
}
