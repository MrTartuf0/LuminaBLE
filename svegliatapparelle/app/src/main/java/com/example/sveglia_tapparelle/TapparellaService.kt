package com.example.sveglia_tapparelle

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class TapparellaService : Service() {

    private var bluetoothGatt: BluetoothGatt? = null
    private val CHANNEL_ID = "TapparellaServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Mostra la notifica per mantenere in vita il processo
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Automazione Risveglio")
            .setContentText("Apertura tapparelle in corso...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)

        // Inizia la sequenza BLE
        connectAndOpen()

        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun connectAndOpen() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null || !adapter.isEnabled) {
            stopSelf() // Se il BT è spento, uccide il servizio
            return
        }

        val device = adapter.getRemoteDevice(DEVICE_MAC)
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE_SERVICE", "Connesso in background. Cerco servizi...")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close()
                stopSelf() // Lavoro finito, si spegne
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val characteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID)
                if (characteristic != null) {
                    val payload = byteArrayOf(0x01) // Comando APRI
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(characteristic, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    } else {
                        characteristic.value = payload
                        gatt.writeCharacteristic(characteristic)
                    }
                    Log.d("BLE_SERVICE", "Comando di apertura inviato dal background!")

                    // Disconnette e chiude il servizio
                    gatt.disconnect()
                } else {
                    stopSelf()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Servizio Tapparella Automatica",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}