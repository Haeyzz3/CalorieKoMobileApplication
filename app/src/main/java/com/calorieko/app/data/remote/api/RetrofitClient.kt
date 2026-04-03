package com.calorieko.app.data.remote.api

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client configured for the CalorieKo Laravel backend.
 *
 * Features:
 * - Automatically injects the current Firebase ID token into every request.
 * - Logs HTTP requests/responses in debug builds.
 * - 30-second timeout for the potentially large sync payload.
 *
 * Usage:
 * ```
 * val api = RetrofitClient.getInstance(baseUrl).create(CalorieKoApiService::class.java)
 * ```
 */
object RetrofitClient {

    private const val TAG = "RetrofitClient"

    /**
     * The base URL for the CalorieKo API.
     * This should be set before calling [getApiService].
     * Format: "https://your-domain.com/" (must end with /)
     */
    @Volatile
    private var apiService: CalorieKoApiService? = null

    @Volatile
    private var currentBaseUrl: String? = null

    /**
     * Returns a configured [CalorieKoApiService] instance.
     *
     * @param baseUrl The base URL for the API (e.g., "https://calorieko.example.com/").
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

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

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
}
