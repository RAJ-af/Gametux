package com.gametux.core.signaling

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class SignalingServer(private val port: Int) {
    private val TAG = "SignalingServer"
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start(onMessageReceived: (message: String, responseWriter: (String) -> Unit) -> Unit) {
        if (isRunning) return
        isRunning = true
        thread {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "Signaling Server started on port $port")
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    handleClient(clientSocket, onMessageReceived)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
            }
        }
    }

    private fun handleClient(socket: Socket, onMessageReceived: (String, (String) -> Unit) -> Unit) {
        thread {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)
                val line = reader.readLine()
                if (line != null) {
                    onMessageReceived(line) { response ->
                        writer.println(response)
                    }
                }
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Client handling error", e)
            }
        }
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
        serverSocket = null
    }
}
