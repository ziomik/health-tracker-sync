package com.healthtracker.sync.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore per configurazione app
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "health_sync_prefs")

class PreferencesManager(private val context: Context) {
    
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val deviceIdKey = stringPreferencesKey("device_id")
    private val apiTokenKey = stringPreferencesKey("api_token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val lastSyncKey = stringPreferencesKey("last_sync_timestamp")
    
    val serverUrl: Flow<String?> = context.dataStore.data.map { it[serverUrlKey] }
    val deviceId: Flow<String?> = context.dataStore.data.map { it[deviceIdKey] }
    val apiToken: Flow<String?> = context.dataStore.data.map { it[apiTokenKey] }
    val userId: Flow<String?> = context.dataStore.data.map { it[userIdKey] }
    val lastSync: Flow<String?> = context.dataStore.data.map { it[lastSyncKey] }
    
    suspend fun saveSetupData(
        serverUrl: String,
        deviceId: String,
        apiToken: String,
        userId: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[serverUrlKey] = serverUrl
            prefs[deviceIdKey] = deviceId
            prefs[apiTokenKey] = apiToken
            prefs[userIdKey] = userId
        }
    }
    
    suspend fun updateLastSync(timestamp: String) {
        context.dataStore.edit { prefs ->
            prefs[lastSyncKey] = timestamp
        }
    }
    
    suspend fun clearSetup() {
        context.dataStore.edit { prefs ->
            prefs.remove(serverUrlKey)
            prefs.remove(deviceIdKey)
            prefs.remove(apiTokenKey)
            prefs.remove(userIdKey)
            prefs.remove(lastSyncKey)
        }
    }
    
    suspend fun isConfigured(): Boolean {
        var configured = false
        context.dataStore.data.map { prefs ->
            configured = prefs[deviceIdKey] != null && 
                        prefs[apiTokenKey] != null &&
                        prefs[serverUrlKey] != null
        }.collect { }
        return configured
    }
}
