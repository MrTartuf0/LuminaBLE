package com.example.sveglia_tapparelle

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.bluetooth.*
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

const val DEVICE_MAC = "7C:9E:BD:F9:37:DA"
val SERVICE_UUID: UUID = UUID.fromString("19b10000-e8f2-537e-4f6c-d104768a1214")
val CHAR_UUID: UUID = UUID.fromString("19b10001-e8f2-537e-4f6c-d104768a1214")

class MainActivity : ComponentActivity() {

    private var bluetoothGatt: BluetoothGatt? = null
    private val isConnected = mutableStateOf(false)
    private val statusText = mutableStateOf("Pronto per la connessione")

    // Nuovo stato reattivo per la sveglia
    private val alarmText = mutableStateOf("Caricamento sveglia...")

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.BLUETOOTH_CONNECT] == true) {
            connectToTapparella()
        } else {
            statusText.value = "Permessi negati!"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lettura iniziale
        refreshAlarm()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TapparellaDashboard(
                        nextAlarmTime = alarmText.value,
                        isConnected = isConnected.value,
                        statusMessage = statusText.value,
                        onRefreshAlarm = { refreshAlarm() },
                        onConnectClick = { checkPermissionsAndConnect() },
                        onDisconnectClick = { disconnect() },
                        onSendApri = { sendCommand(byteArrayOf(0x01)) },
                        onSendChiudi = { sendCommand(byteArrayOf(0x02)) },
                        onArmAutomazione = { scheduleTapparellaAutomated() } // <--- AGGIUNGI QUESTO
                    )
                }
            }
        }
    }

    // Funzione isolata per aggiornare la UI e stampare log
    private fun refreshAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextAlarmClock = alarmManager.nextAlarmClock

        if (nextAlarmClock != null) {
            val date = Date(nextAlarmClock.triggerTime)
            val orario = DateFormat.getTimeFormat(this).format(date)
            alarmText.value = "Sveglia rilevata: $orario"
            Log.d("DEBUG_SVEGLIA", "AlarmManager vede una sveglia alle: $orario (Timestamp: ${nextAlarmClock.triggerTime})")
        } else {
            alarmText.value = "Nessuna sveglia di sistema trovata"
            Log.d("DEBUG_SVEGLIA", "AlarmManager ha restituito NULL. Il sistema non vede sveglie.")
        }
    }

    private fun checkPermissionsAndConnect() {
        requestPermissionLauncher.launch(
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        )
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleTapparellaAutomated() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Controllo specifico per Android 12+ (Pixel 5)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Se non hai il permesso, apre la schermata delle impostazioni di Android
                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                statusText.value = "Attiva il permesso e riprova!"
                return
            }
        }

        val nextAlarmClock = alarmManager.nextAlarmClock

        if (nextAlarmClock != null) {
            val triggerTime = nextAlarmClock.triggerTime
            val intent = android.content.Intent(this, TapparellaReceiver::class.java)

            // PendingIntent con i flag corretti per le versioni recenti di Android
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // Imposta l'allarme che "buca" il Doze Mode
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )

            statusText.value = "Automazione ARMATA! 🚀"
            Log.d("ALARM_SETUP", "Automazione programmata per il timestamp: $triggerTime")
        } else {
            statusText.value = "Nessuna sveglia di sistema da agganciare."
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToTapparella() {
        statusText.value = "Connessione in corso..."
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        if (adapter == null || !adapter.isEnabled) return
        val device = adapter.getRemoteDevice(DEVICE_MAC)
        bluetoothGatt = device.connectGatt(this, true, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                statusText.value = "Dispositivo agganciato!"
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected.value = false
                statusText.value = "Disconnesso"
                gatt.close()
            }
        }
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                isConnected.value = true
                statusText.value = "Pronto! Tapparella operativa."
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendCommand(payload: ByteArray) {
        if (!isConnected.value) return
        val characteristic = bluetoothGatt?.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID)
        if (characteristic != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(characteristic, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                characteristic.value = payload
                bluetoothGatt?.writeCharacteristic(characteristic)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun disconnect() {
        bluetoothGatt?.disconnect()
    }
}

@Composable
fun TapparellaDashboard(
    nextAlarmTime: String,
    isConnected: Boolean,
    statusMessage: String,
    onRefreshAlarm: () -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onSendApri: () -> Unit,
    onSendChiudi: () -> Unit,
    onArmAutomazione: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = nextAlarmTime, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

        // Tasto per forzare la lettura
        TextButton(onClick = onRefreshAlarm) {
            Text("🔄 Rileggi Sveglia ORA")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = statusMessage, fontSize = 16.sp, color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSendApri, enabled = isConnected, modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(16.dp)
        ) { Text("CHIUDI", fontSize = 24.sp, fontWeight = FontWeight.Bold) }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSendChiudi, enabled = isConnected, modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) { Text("APRI", fontSize = 24.sp, fontWeight = FontWeight.Bold) }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onArmAutomazione,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text("ARMA AUTOMAZIONE MATTUTINA", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedButton(onClick = if (isConnected) onDisconnectClick else onConnectClick) { Text(if (isConnected) "Disconnetti Arduino" else "Connetti Arduino") }
    }
}