package com.healthtracker.sync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.HealthConnectClient as HCClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
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

    // Health Connect permissions - uso approccio manuale con Intent
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Toast.makeText(this, "📋 Tornato da Health Connect! Verifico permessi...", Toast.LENGTH_LONG).show()
        
        // Ricontrolla se permessi sono stati concessi
        lifecycleScope.launch {
            checkAndSyncIfPermitted()
        }
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
                    requestHealthConnectPermissions()
                }) {
                    Text("Richiedi Permessi Health Connect")
                }

                Spacer(modifier = Modifier.height(8.dp))

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

                // Request Health Connect permissions DOPO setup
                // Non fare sync subito, aspetta che utente conceda permessi
                requestHealthConnectPermissions()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Show error
        }
    }

    private fun requestHealthConnectPermissions() {
        // Verifica se Health Connect è disponibile
        val availability = HealthConnectClient.getSdkStatus(this)
        
        Toast.makeText(this, "SDK Status: $availability (Android ${android.os.Build.VERSION.SDK_INT})", Toast.LENGTH_LONG).show()
        
        when (availability) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                Toast.makeText(this, "⚠️ Health Connect non disponibile", Toast.LENGTH_LONG).show()
                return
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Toast.makeText(this, "⚠️ Aggiorna Health Connect", Toast.LENGTH_LONG).show()
                return
            }
            else -> {
                Toast.makeText(this, "🚀 Trigger richiesta permessi via tentativo lettura...", Toast.LENGTH_SHORT).show()
                
                lifecycleScope.launch {
                    try {
                        // Tentativo di leggere dati HC - questo TRIGGERA automaticamente popup permessi!
                        val healthClient = HealthConnectClient.getOrCreate(this@MainActivity)
                        
                        // Controlla prima quali permessi abbiamo
                        val granted = healthClient.permissionController.getGrantedPermissions()
                        
                        if (granted.isEmpty()) {
                            Toast.makeText(this@MainActivity, "📋 Nessun permesso ancora. Tento lettura per triggerare popup...", Toast.LENGTH_LONG).show()
                            
                            // Tento lettura dati - questo DEVE far apparire popup permessi!
                            healthManager.readVitalsData()
                            
                            Toast.makeText(this@MainActivity, "✅ Popup permessi dovrebbe apparire ora!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MainActivity, "✅ Già ${granted.size} permessi concessi!", Toast.LENGTH_SHORT).show()
                        }
                        
                        // Ricontrolla dopo tentativo lettura
                        checkAndSyncIfPermitted()
                        
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "❌ Errore: ${e.message}", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    private suspend fun checkAndSyncIfPermitted() {
        try {
            val healthClient = HealthConnectClient.getOrCreate(this)
            val granted = healthClient.permissionController.getGrantedPermissions()
            
            val allGranted = healthManager.requiredPermissions.all { it in granted }
            
            Toast.makeText(this, "📊 Permessi concessi: ${granted.size}/${healthManager.requiredPermissions.size}", Toast.LENGTH_LONG).show()
            
            if (allGranted) {
                Toast.makeText(this, "✅ Tutti permessi OK! Avvio sync...", Toast.LENGTH_SHORT).show()
                manualSync()
            } else {
                val missing = healthManager.requiredPermissions - granted
                Toast.makeText(this, "⚠️ Mancano ${missing.size} permessi. Riprova a concederli.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Errore check permessi: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
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
