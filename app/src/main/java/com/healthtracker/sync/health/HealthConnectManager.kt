package com.healthtracker.sync.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.healthtracker.sync.api.ActivityData
import com.healthtracker.sync.api.SleepData
import com.healthtracker.sync.api.VitalData
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Manager per leggere dati da Health Connect
 */
class HealthConnectManager(private val context: Context) {
    
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    
    // Permissions richieste
    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class)
    )
    
    /**
     * Leggi dati vitali dalle ultime 24 ore
     */
    suspend fun readVitalsData(): List<VitalData> {
        val vitals = mutableListOf<VitalData>()
        val endTime = Instant.now()
        val startTime = endTime.minusSeconds(24 * 3600)  // Ultime 24h
        
        try {
            // Heart Rate
            val hrRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            hrRecords.records.forEach { record ->
                record.samples.forEach { sample ->
                    vitals.add(VitalData(
                        type = "heart_rate",
                        value = sample.beatsPerMinute,
                        timestamp = sample.time.toString()
                    ))
                }
            }
            
            // SpO2
            val spo2Records = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    OxygenSaturationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            spo2Records.records.forEach { record ->
                vitals.add(VitalData(
                    type = "spo2",
                    value = record.percentage.value.toInt(),
                    timestamp = record.time.toString()
                ))
            }
            
            // Blood Pressure
            val bpRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    BloodPressureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            bpRecords.records.forEach { record ->
                vitals.add(VitalData(
                    type = "blood_pressure",
                    systolic = record.systolic.inMillimetersOfMercury.toInt(),
                    diastolic = record.diastolic.inMillimetersOfMercury.toInt(),
                    timestamp = record.time.toString()
                ))
            }
            
            // Blood Glucose
            val glucoseRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    BloodGlucoseRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            glucoseRecords.records.forEach { record ->
                vitals.add(VitalData(
                    type = "glucose",
                    value = record.level.inMilligramsPerDeciliter,
                    timestamp = record.time.toString()
                ))
            }
            
            // Temperature
            val tempRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    BodyTemperatureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            tempRecords.records.forEach { record ->
                vitals.add(VitalData(
                    type = "temperature",
                    value = record.temperature.inCelsius,
                    timestamp = record.time.toString()
                ))
            }
            
            // Weight
            val weightRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            weightRecords.records.forEach { record ->
                vitals.add(VitalData(
                    type = "weight",
                    value = record.weight.inKilograms,
                    timestamp = record.time.toString()
                ))
            }
            
            // Respiration Rate
            val respRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    RespiratoryRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            respRecords.records.forEach { record ->
                vitals.add(VitalData(
                    type = "respiration_rate",
                    value = record.rate,
                    timestamp = record.time.toString()
                ))
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return vitals
    }
    
    /**
     * Leggi dati sonno dalle ultime 7 giorni
     */
    suspend fun readSleepData(): List<SleepData> {
        val sleepList = mutableListOf<SleepData>()
        val endTime = Instant.now()
        val startTime = endTime.minusSeconds(7 * 24 * 3600)  // Ultimi 7 giorni
        
        try {
            val sleepRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            
            sleepRecords.records.forEach { record ->
                val durationHours = (record.endTime.epochSecond - record.startTime.epochSecond) / 3600.0
                
                sleepList.add(SleepData(
                    startTime = record.startTime.toString(),
                    endTime = record.endTime.toString(),
                    durationHours = durationHours,
                    qualityScore = null,  // Calcolato dal backend se disponibile
                    deepSleepHours = null,  // Da stages se disponibili
                    lightSleepHours = null,
                    remHours = null,
                    awakeHours = null
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return sleepList
    }
    
    /**
     * Leggi dati attività dalle ultime 24 ore
     */
    suspend fun readActivityData(): List<ActivityData> {
        val activities = mutableListOf<ActivityData>()
        val endTime = Instant.now()
        val startTime = endTime.minusSeconds(24 * 3600)
        
        try {
            // Steps
            val stepsRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            
            stepsRecords.records.forEach { record ->
                val durationMinutes = ((record.endTime.epochSecond - record.startTime.epochSecond) / 60).toInt()
                
                activities.add(ActivityData(
                    type = "walking",
                    durationMinutes = durationMinutes,
                    steps = record.count.toInt(),
                    timestamp = record.startTime.toString()
                ))
            }
            
            // Distance
            val distanceRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    DistanceRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            
            // Calories
            val caloriesRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            
            // Merge distance and calories con steps (matching timestamp)
            distanceRecords.records.forEach { distRecord ->
                activities.find { it.timestamp == distRecord.startTime.toString() }?.let { activity ->
                    // Trova attività corrispondente e aggiungi distanza
                    val index = activities.indexOf(activity)
                    activities[index] = activity.copy(distanceKm = distRecord.distance.inKilometers)
                }
            }
            
            caloriesRecords.records.forEach { calRecord ->
                activities.find { it.timestamp == calRecord.startTime.toString() }?.let { activity ->
                    val index = activities.indexOf(activity)
                    activities[index] = activity.copy(calories = calRecord.energy.inKilocalories.toInt())
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return activities
    }
}
