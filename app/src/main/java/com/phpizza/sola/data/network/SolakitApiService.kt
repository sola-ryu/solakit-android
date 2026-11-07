package com.phpizza.sola.data.network

import com.google.gson.JsonObject
import com.phpizza.sola.data.network.model.ApiSessionDto
import com.phpizza.sola.data.network.model.CreateSessionRequestDto
import com.phpizza.sola.data.network.model.RunRequestDto
import com.phpizza.sola.data.network.model.UpdateSessionRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SolakitApiService {

    @GET("api/sessions")
    suspend fun getSessions(
        @Query("status") status: String = "active",
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 50,
        @Header("authorization") authorization: String? = null
    ): List<ApiSessionDto>

    @POST("api/sessions")
    suspend fun createSession(
        @Body request: CreateSessionRequestDto,
        @Header("authorization") authorization: String? = null
    ): ApiSessionDto

    @GET("api/sessions/{session_id}")
    suspend fun getSession(
        @Path("session_id") sessionId: String,
        @Header("authorization") authorization: String? = null
    ): ApiSessionDto

    @PATCH("api/sessions/{session_id}")
    suspend fun updateSession(
        @Path("session_id") sessionId: String,
        @Body request: UpdateSessionRequestDto,
        @Header("authorization") authorization: String? = null
    ): ApiSessionDto

    @POST("api/sessions/{session_id}/archive")
    suspend fun archiveSession(
        @Path("session_id") sessionId: String,
        @Header("authorization") authorization: String? = null
    ): JsonObject

    @POST("api/sessions/{session_id}/messages")
    suspend fun sendMessage(
        @Path("session_id") sessionId: String,
        @Body request: RunRequestDto,
        @Query("wait") wait: Boolean = true,
        @Query("stream") stream: Boolean = false,
        @Header("authorization") authorization: String? = null
    ): JsonObject

    @GET("api/runs")
    suspend fun getRuns(
        @Query("session_id") sessionId: String? = null,
        @Header("authorization") authorization: String? = null
    ): List<JsonObject>

    @GET("api/runs/{run_id}")
    suspend fun getRunRecord(
        @Path("run_id") runId: String,
        @Query("messages") messages: Boolean = true,
        @Header("authorization") authorization: String? = null
    ): JsonObject
}
