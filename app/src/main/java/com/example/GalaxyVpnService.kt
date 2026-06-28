package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.File
// Laptop တွင် libv2ray.aar ထည့်ပြီးပါက ဤ import ပွင့်လာမည်ဖြစ်သည်
import libv2ray.Libv2ray

class GalaxyVpnService : VpnService() {

    private var isRunning = false
    private var vpnInterface: ParcelFileDescriptor? = null
    private val CHANNEL_ID = "GalaxyVpnChannel"
    private val NOTIFICATION_ID = 7771

    companion object {
        const val ACTION_START = "com.example.ACTION_START"
        const val ACTION_STOP = "com.example.ACTION_STOP"
        const val EXTRA_V2RAY_CONFIG = "EXTRA_V2RAY_CONFIG"
        const val EXTRA_SERVER_NAME = "EXTRA_SERVER_NAME"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopV2RayEngine()
            return START_NOT_STICKY
        } else if (action == ACTION_START) {
            val configJson = intent.getStringExtra(EXTRA_V2RAY_CONFIG) ?: ""
            val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Galaxy Node"
            if (configJson.isNotEmpty()) {
                startV2RayEngine(configJson, serverName)
            }
        }
        return START_STICKY
    }

    private fun startV2RayEngine(configJson: String, serverName: String) {
        if (isRunning) return
        isRunning = true

        try {
            // ၁။ V2Ray Core Engine အား JSON Config ဖြင့် အရင်ဆုံး စတင်နှိုးခြင်း
            Libv2ray.startCore(configJson)

            // ၂။ Android ရဲ့ VPN Tunnel လမ်းကြောင်းကို တည်ဆောက်ခြင်း
            val builder = Builder()
            vpnInterface = builder.setSession("Galaxy Tunnel")
                .addAddress("26.26.26.1", 30) // Local Virtual IP
                .addRoute("0.0.0.0", 0)       // ဖုန်းတစ်ခုလုံး၏ Traffic ကို လမ်းကြောင်းလွှဲခြင်း
                .addDnsServer("8.8.8.8")
                .setMtu(1500)
                .establish()

            // ၃။ ရလာသော File Descriptor (fd) အား V2Ray Core ထံ သို့ လွှဲပြောင်းပေးရမည်
            val fd = vpnInterface?.fd ?: -1
            if (fd != -1) {
                Libv2ray.bindFd(fd)
            }

            // Notification ပြသခြင်းစနစ်
            showNotification(serverName)

        } catch (e: Exception) {
            e.printStackTrace()
            stopV2RayEngine()
        }
    }

    private fun showNotification(serverName: String) {
        createNotificationChannel()
        val stopIntent = Intent(this, GalaxyVpnService::class.java).apply { action = ACTION_STOP }
        val pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Galaxy Tunnel (V2Ray Core)")
            .setContentText("Connected to $serverName")
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", pendingStopIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopV2RayEngine() {
        isRunning = false
        try {
            // V2Ray Core အား ရပ်တန့်ခြင်း
            Libv2ray.stopCore()
            
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopV2RayEngine()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Galaxy V2Ray Service", NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
