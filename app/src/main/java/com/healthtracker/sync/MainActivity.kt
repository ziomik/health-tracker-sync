package com.healthtracker.sync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.healthtracker.sync.api.HealthTrackerApi
import com.healthtracker.sync.api.SetupQRData
import com.healthtracker.sync.data.PreferencesManager
import com.healthtracker.sync.health.HealthConnectManager
import com.healthtracker.sync.ui.theme.HealthTrackerSyncTheme
import com.healthtracker.sync.work.HealthSyncWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    
    private lateinit var prefsManager: PreferencesManager
    private lateinit var healthManager: HealthConnectManager
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
    }
    
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { qrContent ->
            handleQRScan(qrContent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefsManager = PreferencesManager(this)
        healthManager = HealthConnectManager(this)
        
        setContent {
            HealthTrackerSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
    
    @Composable
    fun MainScreen() {
        val scope = rememberCoroutineScope()
        
        var isConfigured by remember { mutableStateOf(false) }
        var serverUrl by remember { mutableStateOf("") }
        var lastSync by remember { mutableStateOf<String?>(null) }
        var isSyncing by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            prefsManager.serverUrl.collect { serverUrl = it ?: "" }
        }
        
        LaunchedEffect(Unit) {
            prefsManager.lastSync.collect { lastSync = it }
        }
        
        LaunchedEffect(Unit) {
            isConfigured = prefsManager.deviceId.first() != null
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Health Tracker Sync",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (!isConfigured) {
                // Setup screen
                Text(
                    text = "📱 Scansiona QR code dalla webapp",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = { launchQRScanner() }) {
                    Text("Scansiona QR Code")
                }
            } else {
                // Main screen
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✅ Configurato",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Server: $serverUrl",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (lastSync != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ultima sync: ${formatTimestamp(lastSync!!)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            isSyncing = true
                            manualSync()
                            isSyncing = false
                        }
                    },
                    enabled = !isSyncing
                ) {
                    Text(if (isSyncing) "Sincronizzazione..." else "Sincronizza Ora")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(onClick = {
                    scope.launch {
                        prefsManager.clearSetup()
                        isConfigured = false
                    }
                }) {
                    Text("Reset Configurazione")
                }
            }
        }
    }
    
    private fun launchQRScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scansiona QR code dalla webapp")
            setBeepEnabled(true)
            setOrientationLocked(false)
        }
        qrScannerLauncher.launch(options)
    }
    
    private fun handleQRScan(qrContent: String) {
        try {
            val setupData = Gson().fromJson(qrContent, SetupQRData::class.java)
            
            lifecycleScope.launch {
                prefsManager.saveSetupData(
                    serverUrl = setupData.serverUrl,
                    deviceId = setupData.deviceId,
                    apiToken = setupData.apiToken,
                    userId = setupData.userId.toString()
                )
                
                // Inizializza API
                HealthTrackerApi.initialize(setupData.serverUrl)
                
                // Request Health Connect permissions
                requestHealthConnectPermissions()
                
                // Trigger immediate sync
                manualSync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Show error
        }
    }
    
    private fun requestHealthConnectPermissions() {
        permissionLauncher.launch(healthManager.requiredPermissions.toTypedArray())
    }
    
    private fun manualSync() {
        val syncWork = OneTimeWorkRequestBuilder<HealthSyncWorker>().build()
        WorkManager.getInstance(this).enqueue(syncWork)
    }
    
    private fun formatTimestamp(iso: String): String {
        return try {
            val instant = Instant.parse(iso)
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } catch (e: Exception) {
            iso
        }
    }
}
