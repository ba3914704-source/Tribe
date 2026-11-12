package com.example.core.network

import com.example.BuildConfig
import com.example.core.security.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class SupabaseAuthInterceptor(
    private val sessionManager: SessionManager,
    private val publishableKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Always include the Supabase Publishable Key in the 'apikey' header
        if (publishableKey.isNotBlank()) {
            requestBuilder.header("apikey", publishableKey)
        }

        // Only attach Authorization Bearer if an authenticated user session access token exists
        val accessToken = runBlocking { sessionManager.getAccessToken() }
        if (!accessToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        return chain.proceed(requestBuilder.build())
    }
}

object SupabaseNetworkProvider {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun createAuthApi(
        baseUrl: String = BuildConfig.SUPABASE_URL.ifBlank { "https://cididsanututlefebehw.supabase.co" },
        publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        sessionManager: SessionManager
    ): SupabaseAuthApi {
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(SupabaseAuthInterceptor(sessionManager, publishableKey))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(SupabaseAuthApi::class.java)
    }

    fun createRestApi(
        baseUrl: String = BuildConfig.SUPABASE_URL.ifBlank { "https://cididsanututlefebehw.supabase.co" },
        publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        sessionManager: SessionManager
    ): SupabaseRestApi {
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(SupabaseAuthInterceptor(sessionManager, publishableKey))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(SupabaseRestApi::class.java)
    }
}
