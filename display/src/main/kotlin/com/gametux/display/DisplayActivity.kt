package com.gametux.display

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.gametux.core.discovery.DiscoveryManager
import com.gametux.core.signaling.SignalingServer
import com.gametux.core.webrtc.WebRTCManager
import com.gametux.core.webrtc.WebRTCReceiver
import com.gametux.display.databinding.ActivityDisplayBinding
import org.webrtc.EglBase
import org.webrtc.RendererCommon

class DisplayActivity : FragmentActivity() {
    private lateinit var binding: ActivityDisplayBinding
    private lateinit var discoveryManager: DiscoveryManager
    private lateinit var signalingServer: SignalingServer
    private lateinit var webRTCManager: WebRTCManager
    private var webRTCReceiver: WebRTCReceiver? = null
    private val eglBase = EglBase.create()

    private val SIGNALING_PORT = 9999

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initWebRTC()
        startServices()
    }

    private fun initWebRTC() {
        binding.videoRender.init(eglBase.eglBaseContext, null)
        binding.videoRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        binding.videoRender.setEnableHardwareScaler(true)

        webRTCManager = WebRTCManager(this)
    }

    private fun startServices() {
        discoveryManager = DiscoveryManager(this)
        discoveryManager.startAdvertising(SIGNALING_PORT)

        signalingServer = SignalingServer(SIGNALING_PORT)
        signalingServer.start { message, responseWriter ->
            handleSignalingMessage(message, responseWriter)
        }
    }

    private fun handleSignalingMessage(message: String, responseWriter: (String) -> Unit) {
        // Simple protocol: OFFER:<sdp> or ICE:<candidate>
        when {
            message.startsWith("OFFER:") -> {
                val offerSdp = message.substringAfter("OFFER:")
                runOnUiThread {
                    if (webRTCReceiver == null) {
                        webRTCReceiver = WebRTCReceiver(this, webRTCManager) { videoTrack ->
                            videoTrack.addSink(binding.videoRender)
                            runOnUiThread {
                                binding.waitingLayout.visibility = View.GONE
                            }
                        }
                    }
                    webRTCReceiver?.handleOffer(
                        offerSdp,
                        onAnswerCreated = { answerSdp ->
                            responseWriter("ANSWER:$answerSdp")
                        },
                        onIceCandidate = { candidateSdp ->
                            // In this simple one-way signaling, we don't have a good way to push ICE to phone
                            // unless we keep the socket open or the phone polls.
                            // For LAN, often just the answer is enough if host candidates are in the SDP.
                        }
                    )
                }
            }
            message.startsWith("ICE:") -> {
                val candidateSdp = message.substringAfter("ICE:")
                runOnUiThread {
                    webRTCReceiver?.addIceCandidate(candidateSdp)
                }
                responseWriter("OK")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        discoveryManager.stopAdvertising()
        signalingServer.stop()
        webRTCReceiver?.stop()
        webRTCManager.dispose()
        binding.videoRender.release()
        eglBase.release()
    }
}
