package com.gametux.core.signaling

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

class SignalingClient(private val host: String, private val port: Int) {
    private val TAG = "SignalingClient"

    fun sendMessage(message: String, onResponse: (String) -> Unit) {
        thread {
            try {
                val socket = Socket(host, port)
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                writer.println(message)
                val response = reader.readLine()
                if (response != null) {
                    onResponse(response)
                }
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message to $host:$port", e)
            }
        }
    }
}
