package com.phpizza.sola.data.network

import com.phpizza.sola.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val DEFAULT_BASE_URL = "http://127.0.0.1:8382/"

    private var baseUrl: String = DEFAULT_BASE_URL
    private var apiToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        apiToken?.takeIf { it.isNotBlank() }?.let { token ->
            val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
            builder.header("authorization", authHeader)
        }

        chain.proceed(builder.build())
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("authorization")
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private var retrofit: Retrofit? = null
    private var apiService: SolakitApiService? = null

    fun getService(customBaseUrl: String? = null, token: String? = null): SolakitApiService {
        val targetUrl = sanitizeUrl(customBaseUrl ?: baseUrl)
        val targetToken = token ?: apiToken

        if (apiService == null || baseUrl != targetUrl || apiToken != targetToken) {
            baseUrl = targetUrl
            apiToken = targetToken

            retrofit = Retrofit.Builder()
                .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit!!.create(SolakitApiService::class.java)
        }
        return apiService!!
    }

    fun configure(url: String, token: String) {
        baseUrl = sanitizeUrl(url)
        apiToken = token.trim()
        apiService = null
    }

    private fun sanitizeUrl(url: String): String {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "http://$cleanUrl"
        }
        return cleanUrl
    }

    fun getBaseUrl(): String = baseUrl
    fun getApiToken(): String? = apiToken
}
