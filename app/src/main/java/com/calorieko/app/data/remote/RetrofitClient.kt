package com.calorieko.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client for the CalorieKo Laravel API.
 *
 * Every outgoing request is automatically decorated with the current
 * Firebase user's ID token via an OkHttp interceptor, so controllers
 * on the backend can verify identity through the `firebase.auth` middleware.
 *
 * Usage:
 *   val api = RetrofitClient.api
 *   api.syncProfile(profile)
 */
object RetrofitClient {

    // ── Change this to your production URL when deploying ──
    // For Android emulator → use "http://10.0.2.2:8000/api/"
    // For a physical device on the same Wi-Fi → use your PC's local IP
    private const val BASE_URL = "http://192.168.150.50:8000/api/"

    /**
     * OkHttp interceptor that attaches the Firebase ID token
     * as a Bearer token on every request.
     */
    private val firebaseAuthInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()

        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                // getIdToken(false) returns a cached token; force-refresh only when expired
                val tokenResult = com.google.android.gms.tasks.Tasks.await(
                    user.getIdToken(false)
                )
                val token = tokenResult.token
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            }
        } catch (e: Exception) {
            // If token retrieval fails, send the request without auth
            // The server will return 401 and the app can handle it
            e.printStackTrace()
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(firebaseAuthInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /** The ready-to-use API instance. */
    val api: CalorieKoApi = retrofit.create(CalorieKoApi::class.java)
}
