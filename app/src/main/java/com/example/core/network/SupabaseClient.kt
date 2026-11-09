package com.example.core.network

import com.example.BuildConfig
import com.example.core.security.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class SupabaseAuthInterceptor(
    private val sessionManager: SessionManager,
    private val publishableKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        if (publishableKey.isNotBlank()) requestBuilder.header("apikey", publishableKey)

        val accessToken = runBlocking { sessionManager.getAccessToken() }
        if (!accessToken.isNullOrBlank()) requestBuilder.header("Authorization", "Bearer $accessToken")

        return chain.proceed(requestBuilder.build())
    }
}

object SupabaseNetworkProvider {
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    fun createAuthApi(
        baseUrl: String = BuildConfig.SUPABASE_URL.ifBlank { "https://cididsanututlefebehw.supabase.co" },
        publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        sessionManager: SessionManager
    ): SupabaseAuthApi {
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(SupabaseAuthInterceptor(sessionManager, publishableKey))
            .build()
        return Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseAuthApi::class.java)
    }

    fun createRestApi(
        baseUrl: String = BuildConfig.SUPABASE_URL.ifBlank { "https://cididsanututlefebehw.supabase.co" },
        publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        sessionManager: SessionManager
    ): SupabaseRestApi {
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(SupabaseAuthInterceptor(sessionManager, publishableKey))
            .build()
        return Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseRestApi::class.java)
    }
}
