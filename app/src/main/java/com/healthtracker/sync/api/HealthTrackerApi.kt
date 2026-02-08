package com.healthtracker.sync.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * API Service per Health Tracker backend
 */
interface HealthTrackerApiService {
    
    @POST("/api/sync/external/data")
    suspend fun syncData(
        @Header("X-Device-ID") deviceId: String,
        @Header("X-API-Token") apiToken: String,
        @Body request: SyncRequest
    ): Response<SyncResponse>
    
    @GET("/api/sync/external/status")
    suspend fun getStatus(
        @Header("Authorization") token: String
    ): Response<Any>
}

/**
 * API Client singleton
 */
object HealthTrackerApi {
    
    private var retrofit: Retrofit? = null
    private var service: HealthTrackerApiService? = null
    
    fun initialize(baseUrl: String) {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        service = retrofit!!.create(HealthTrackerApiService::class.java)
    }
    
    fun getService(): HealthTrackerApiService {
        return service ?: throw IllegalStateException("API not initialized. Call initialize() first.")
    }
    
    suspend fun syncData(
        deviceId: String,
        apiToken: String,
        vitals: List<VitalData>? = null,
        sleep: List<SleepData>? = null,
        activity: List<ActivityData>? = null
    ): Result<SyncResponse> {
        return try {
            val request = SyncRequest(vitals, sleep, activity)
            val response = getService().syncData(deviceId, apiToken, request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Sync failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
