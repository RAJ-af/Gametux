package com.gametux.console.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gametux.core.discovery.DiscoveryManager
import com.gametux.core.signaling.SignalingClient
import com.gametux.core.signaling.SignalingInterface
import com.gametux.core.webrtc.WebRTCManager
import com.gametux.core.webrtc.WebRTCSender
import org.webrtc.EglBase

class ConsoleService : Service() {
    private val TAG = "ConsoleService"
    private val CHANNEL_ID = "ConsoleServiceChannel"

    private lateinit var discoveryManager: DiscoveryManager
    private lateinit var webRTCManager: WebRTCManager
    private var webRTCSender: WebRTCSender? = null
    private var emulatorCore: com.gametux.console.emulator.EmulatorCore? = null
    private val eglBase = EglBase.create()

    data class DiscoveredDisplay(val host: String, val port: Int)
    private val _discoveredDisplays = mutableListOf<DiscoveredDisplay>()
    val discoveredDisplays: List<DiscoveredDisplay> get() = _discoveredDisplays

    inner class ConsoleBinder : Binder() {
        fun getService(): ConsoleService = this@ConsoleService
    }

    private val binder = ConsoleBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())

        discoveryManager = DiscoveryManager(this)
        webRTCManager = WebRTCManager(this)
    }

    fun startDiscovery(onDisplayListUpdated: () -> Unit) {
        _discoveredDisplays.clear()
        discoveryManager.startDiscovery { host, port ->
            Log.d(TAG, "Found display at $host:$port")
            _discoveredDisplays.add(DiscoveredDisplay(host, port))
            onDisplayListUpdated()
        }
    }

    fun startGame(romPath: String) {
        Log.d(TAG, "Starting game: $romPath")
        if (emulatorCore == null) {
            emulatorCore = com.gametux.console.emulator.EmulatorCore()
        }
    }

    fun connectToDisplay(host: String, port: Int) {
        val signalingClient = SignalingClient(host, port)
        val signalingInterface = object : SignalingInterface {
            override fun sendOffer(sdp: String, onAnswer: (String) -> Unit) {
                signalingClient.sendMessage("OFFER:$sdp") { response ->
                    if (response.startsWith("ANSWER:")) {
                        onAnswer(response.substringAfter("ANSWER:"))
                    }
                }
            }
            override fun sendIceCandidate(candidate: String) {
                signalingClient.sendMessage("ICE:$candidate") { }
            }
            override fun onIceCandidateReceived(callback: (String) -> Unit) {
                // One-way signaling for now
            }
        }

        webRTCSender = WebRTCSender(this, webRTCManager, signalingInterface)
        webRTCSender?.start(eglBase.eglBaseContext) { surface ->
            Log.d(TAG, "Surface ready for emulator")
            emulatorCore?.renderFrame(surface)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Console Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gametux Console")
            .setContentText("Running emulator and streaming...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        discoveryManager.stopDiscovery()
        webRTCSender?.stop()
        webRTCManager.dispose()
        eglBase.release()
    }
}
