package com.gametux.core.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

class DiscoveryManager(private val context: Context) {
    private val TAG = "DiscoveryManager"
    private val SERVICE_TYPE = "_gametux._tcp."
    private val SERVICE_NAME = "GametuxDisplay"
    private val UDP_PORT = 8888

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var isAdvertising = false
    private var isBrowsing = false

    // --- NSD Advertiser (TV Side) ---

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
            Log.d(TAG, "Service registered: ${nsdServiceInfo.serviceName}")
        }
        override fun onRegistrationFailed(nsdServiceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Registration failed: $errorCode")
        }
        override fun onServiceUnregistered(nsdServiceInfo: NsdServiceInfo) {
            Log.d(TAG, "Service unregistered")
        }
        override fun onUnregistrationFailed(nsdServiceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Unregistration failed: $errorCode")
        }
    }

    fun startAdvertising(port: Int) {
        if (isAdvertising) return

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        isAdvertising = true

        // UDP Fallback
        startUdpBroadcast(port)
    }

    fun stopAdvertising() {
        if (!isAdvertising) return
        nsdManager.unregisterService(registrationListener)
        isAdvertising = false
        stopUdpBroadcast()
    }

    // --- NSD Browser (Phone Side) ---

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val discoveredDevices = mutableSetOf<String>()

    fun startDiscovery(onDisplayFound: (host: String, port: Int) -> Unit) {
        if (isBrowsing) return
        discoveredDevices.clear()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType == SERVICE_TYPE && serviceInfo.serviceName.contains(SERVICE_NAME)) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e(TAG, "Resolve failed: $errorCode")
                        }
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val host = serviceInfo.host.hostAddress ?: ""
                            val port = serviceInfo.port
                            val deviceId = "$host:$port"
                            if (!discoveredDevices.contains(deviceId)) {
                                discoveredDevices.add(deviceId)
                                Log.d(TAG, "Service resolved: $deviceId")
                                onDisplayFound(host, port)
                            }
                        }
                    })
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
            }
            override fun onDiscoveryStopped(regType: String) {
                Log.d(TAG, "Discovery stopped")
            }
            override fun onStartDiscoveryFailed(regType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
            override fun onStopDiscoveryFailed(regType: String, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        isBrowsing = true

        // UDP Fallback listener
        startUdpListener(onDisplayFound)
    }

    fun stopDiscovery() {
        if (!isBrowsing) return
        discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        discoveryListener = null
        isBrowsing = false
        stopUdpListener()
    }

    // --- UDP Fallback (TV Side Broadcast) ---
    private var udpBroadcastThread: Thread? = null
    private fun startUdpBroadcast(port: Int) {
        udpBroadcastThread = thread {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                val message = "GAMETUX_DISPLAY:$port"
                val buffer = message.toByteArray()
                val address = InetAddress.getByName("255.255.255.255")
                while (isAdvertising) {
                    val packet = DatagramPacket(buffer, buffer.size, address, UDP_PORT)
                    socket.send(packet)
                    Thread.sleep(2000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "UDP Broadcast error", e)
            }
        }
    }
    private fun stopUdpBroadcast() {
        udpBroadcastThread?.interrupt()
        udpBroadcastThread = null
    }

    // --- UDP Fallback (Phone Side Listener) ---
    private var udpListenerThread: Thread? = null
    private fun startUdpListener(onDisplayFound: (host: String, port: Int) -> Unit) {
        udpListenerThread = thread {
            try {
                val socket = DatagramSocket(UDP_PORT)
                val buffer = ByteArray(1024)
                while (isBrowsing) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    if (message.startsWith("GAMETUX_DISPLAY:")) {
                        val host = packet.address.hostAddress ?: ""
                        val port = message.substringAfter(":").toIntOrNull() ?: 0
                        val deviceId = "$host:$port"
                        if (!discoveredDevices.contains(deviceId)) {
                            discoveredDevices.add(deviceId)
                            onDisplayFound(host, port)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "UDP Listener error", e)
            }
        }
    }
    private fun stopUdpListener() {
        udpListenerThread?.interrupt()
        udpListenerThread = null
    }
}
