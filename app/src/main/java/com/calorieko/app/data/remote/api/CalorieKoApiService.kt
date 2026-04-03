package com.calorieko.app.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the CalorieKo Laravel backend API.
 *
 * All sync routes require a Firebase ID token in the Authorization header.
 * The token is automatically injected by [RetrofitClient]'s auth interceptor.
 */
interface CalorieKoApiService {

    /**
     * Master atomic sync — pushes all local data to the Laravel backend
     * in a single transactional request.
     *
     * Endpoint: POST /api/sync/full
     * Auth: Firebase ID token (Bearer)
     */
    @POST("api/sync/full")
    suspend fun syncFull(
        @retrofit2.http.Header("Authorization") token: String,
        @Body payload: SyncFullPayload
    ): Response<SyncFullResponse>
}
