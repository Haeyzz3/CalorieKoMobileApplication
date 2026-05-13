package com.calorieko.app.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
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

    /**
     * Pulls the full food catalog from the admin server.
     * Used for server → mobile food database sync (full replace for admin-added dishes).
     *
     * Endpoint: GET /api/sync/foods/catalog
     * Auth: Firebase ID token (Bearer)
     */
    @GET("api/sync/foods/catalog")
    suspend fun getFoodCatalog(
        @retrofit2.http.Header("Authorization") token: String
    ): Response<FoodCatalogResponse>
}
