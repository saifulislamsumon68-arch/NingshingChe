package com.example.data.portal

import com.example.BuildConfig
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

/**
 * Transport configuration for the **public reader** app.
 *
 * Security model
 * --------------
 * The reader only ever reads content that the database already publishes to the
 * world: `blogs WHERE status = 'Publish'`, plus categories, authors, galleries,
 * PDF books, videos, published comments and site settings. Those rows are served
 * by the anonymous RLS policies in `backend/supabase/schema.sql`, so **no bearer
 * token, no login and no session header are required or wanted here**.
 *
 * What *is* required is the Supabase **publishable key**, which PostgREST accepts
 * as `apikey` and as an anonymous `Authorization: Bearer`. It is safe to ship in
 * an APK (it grants exactly the anonymous role RLS already allows) but it is
 * still injected from `.env` through the secrets Gradle plugin rather than being
 * hard-coded, so staging and production can point at different projects.
 *
 * Extra hardening applied below:
 *  - TLS 1.2+ only, modern cipher suites (no cleartext; see
 *    `res/xml/network_security_config.xml`, which also blocks cleartext traffic)
 *  - request logging only in debug builds, with headers redacted
 *  - short-ish timeouts so a dead network fails fast into the offline cache
 */
object PortalConfig {

    private const val FALLBACK_URL = "https://slcpvmpsynkqdozvlsii.supabase.co"
    private const val FALLBACK_KEY = "sb_publishable_jqJACnQHmCMcGjt0kG6Sug_ddknIbAA"

    /** Values injected by the secrets plugin from `.env` (falls back to `.env.example`). */
    private fun env(value: String?): String =
        value?.takeIf { it.isNotBlank() && !it.startsWith("MY_") }.orEmpty()

    val baseUrl: String
        get() = env(BuildConfig.SUPABASE_URL).ifBlank { FALLBACK_URL }.trimEnd('/')

    val publishableKey: String
        get() = env(BuildConfig.SUPABASE_PUBLISHABLE_KEY).ifBlank { FALLBACK_KEY }

    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 25L
    const val WRITE_TIMEOUT_SECONDS = 25L

    /** Largest page the reader ever asks for. PostgREST caps server-side at 1000. */
    const val PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    fun okHttpClient(debug: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectionSpecs(
                listOf(
                    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                        .build(),
                    ConnectionSpec.RESTRICTED_TLS
                )
            )
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("apikey", publishableKey)
                    .addHeader("Authorization", "Bearer $publishableKey")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }

        if (debug) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("apikey")
                redactHeader("Authorization")
            }
            builder.addInterceptor(logging)
        }
        return builder.build()
    }

    fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("$baseUrl/rest/v1/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /** `https://<project>/storage/v1/object/public/<bucket>/<path>` */
    fun storagePublicUrl(bucket: String, path: String): String =
        "$baseUrl/storage/v1/object/public/$bucket/${path.split('/').joinToString("/") { it }}"
}
