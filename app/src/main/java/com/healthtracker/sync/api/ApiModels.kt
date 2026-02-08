package com.healthtracker.sync.api

import com.google.gson.annotations.SerializedName

/**
 * Models per API Health Tracker
 */

data class SetupQRData(
    @SerializedName("server_url") val serverUrl: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("api_token") val apiToken: String,
    @SerializedName("user_id") val userId: Int
)

data class SyncRequest(
    val vitals: List<VitalData>? = null,
    val sleep: List<SleepData>? = null,
    val activity: List<ActivityData>? = null
)

data class VitalData(
    val type: String,  // heart_rate, spo2, blood_pressure, glucose, temperature, weight, respiration_rate
    val value: Number? = null,
    val systolic: Int? = null,  // Per blood_pressure
    val diastolic: Int? = null,  // Per blood_pressure
    val timestamp: String
)

data class SleepData(
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("duration_hours") val durationHours: Double,
    @SerializedName("quality_score") val qualityScore: Int? = null,
    @SerializedName("deep_sleep_hours") val deepSleepHours: Double? = null,
    @SerializedName("light_sleep_hours") val lightSleepHours: Double? = null,
    @SerializedName("rem_hours") val remHours: Double? = null,
    @SerializedName("awake_hours") val awakeHours: Double? = null
)

data class ActivityData(
    val type: String,  // walking, running, cycling, etc.
    @SerializedName("duration_minutes") val durationMinutes: Int,
    val calories: Int? = null,
    @SerializedName("distance_km") val distanceKm: Double? = null,
    val steps: Int? = null,
    val timestamp: String
)

data class SyncResponse(
    val status: String,
    val synced: SyncedCounts,
    @SerializedName("total_records") val totalRecords: Int,
    val message: String
)

data class SyncedCounts(
    val vitals: Int,
    val sleep: Int,
    val activity: Int
)

data class ErrorResponse(
    val error: String
)
