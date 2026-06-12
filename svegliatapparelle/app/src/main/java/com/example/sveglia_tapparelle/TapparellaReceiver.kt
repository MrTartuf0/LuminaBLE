package com.example.sveglia_tapparelle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class TapparellaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, TapparellaService::class.java)

        // Da Android 8 in poi, i servizi in background vanno lanciati esplicitamente come Foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}