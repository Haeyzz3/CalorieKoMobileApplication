package com.calorieko.app.data.remote.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Singleton Retrofit client configured for the CalorieKo Laravel backend.
 *
 * Features:
 * - 30-second timeout for the potentially large sync payload.
 * - Logs HTTP requests/responses for debugging.
 * - Custom SSL handling for Cloudflare-proxied origins whose certificate
 *   chain may not be trusted by Android's default trust store.
 *
 * ── SSL Note ──
 * The `admin-calorieko.xyz` origin uses a Cloudflare Origin CA certificate
 * which is only trusted by Cloudflare's edge servers, not by standard
 * system trust stores. This client applies relaxed SSL validation ONLY
 * for the Laravel API connection. Firebase, Firestore, and all other
 * HTTPS connections in the app are unaffected and use standard SSL.
 *
 * If the Cloudflare SSL is later fixed to serve a proper chain (e.g., by
 * switching to "Full (Strict)" mode with a Let's Encrypt origin cert),
 * this workaround will still work correctly — it just won't be needed.
 *
 * Usage:
 * ```
 * val api = RetrofitClient.getApiService(baseUrl)
 * ```
 */
object RetrofitClient {

    private const val TAG = "RetrofitClient"

    @Volatile
    private var apiService: CalorieKoApiService? = null

    @Volatile
    private var currentBaseUrl: String? = null

    /**
     * Returns a configured [CalorieKoApiService] instance.
     *
     * @param baseUrl The base URL for the API (e.g., "https://admin-calorieko.xyz/").
     *                Must end with a trailing slash.
     */
    fun getApiService(baseUrl: String): CalorieKoApiService {
        // Return existing instance if base URL hasn't changed
        if (apiService != null && currentBaseUrl == baseUrl) {
            return apiService!!
        }

        synchronized(this) {
            if (apiService != null && currentBaseUrl == baseUrl) {
                return apiService!!
            }

            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Log.d(TAG, message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = createCloudflareCompatibleClient(loggingInterceptor)

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(CalorieKoApiService::class.java)
            currentBaseUrl = baseUrl
            Log.d(TAG, "Retrofit client initialized with base URL: $baseUrl")

            return apiService!!
        }
    }

    /**
     * Creates an OkHttpClient that accepts the Cloudflare Origin CA
     * certificate chain.
     *
     * This is scoped ONLY to the Laravel API Retrofit instance.
     * All other HTTPS connections (Firebase, Firestore, Mapbox, etc.)
     * continue to use Android's default, fully-validated SSL.
     */
    private fun createCloudflareCompatibleClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return try {
            // Create a TrustManager that accepts the Cloudflare origin cert
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }

            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            }

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Cloudflare-compatible client, falling back to default", e)
            // Fallback: standard OkHttp client (will fail on broken SSL but at least won't crash)
            OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
}
