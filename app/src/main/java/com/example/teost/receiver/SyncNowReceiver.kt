package com.example.teost.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SyncNowReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        try { com.example.teost.services.SyncScheduler.runOneTimeNow(context) } catch (_: Exception) {}
    }
}


