package com.gametux.core.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRTCSender(
    private val context: Context,
    private val manager: WebRTCManager,
    private val signaling: com.gametux.core.signaling.SignalingInterface
) {
    private val TAG = "WebRTCSender"
    private var peerConnection: PeerConnection? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null

    fun start(eglContext: EglBase.Context, onSurfaceReady: (android.view.Surface) -> Unit) {
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = manager.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                Log.d(TAG, "onIceCandidate: $candidate")
                signaling.sendIceCandidate(candidate.sdp)
            }
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: $newState")
            }
            override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(dataChannel: DataChannel) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {}
        })

        // Add Video Track
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglContext)
        videoSource = manager.createVideoSource(false)
        val videoTrack = manager.createVideoTrack("VIDEO_TRACK", videoSource!!)

        surfaceTextureHelper?.startListening { frame ->
            videoSource?.capturerObserver?.onFrameCaptured(frame)
        }

        val surfaceTexture = surfaceTextureHelper!!.surfaceTexture
        // Set fixed size for emulator (e.g. 1280x720)
        surfaceTexture.setDefaultBufferSize(1280, 720)
        onSurfaceReady(android.view.Surface(surfaceTexture))

        peerConnection?.addTrack(videoTrack)

        // Add Audio Track
        val audioSource = manager.createAudioSource(MediaConstraints())
        val audioTrack = manager.createAudioTrack("AUDIO_TRACK", audioSource)
        peerConnection?.addTrack(audioTrack)

        // Create Offer
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(this, sdp)
                signaling.sendOffer(sdp.description) { answerSdp ->
                    val answer = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
                    peerConnection?.setRemoteDescription(this, answer)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) { Log.e(TAG, "Create Offer Failure: $error") }
            override fun onSetFailure(error: String) { Log.e(TAG, "Set Description Failure: $error") }
        }, MediaConstraints())

        signaling.onIceCandidateReceived { candidateSdp ->
            val candidate = IceCandidate("0", 0, candidateSdp)
            peerConnection?.addIceCandidate(candidate)
        }
    }

    fun stop() {
        surfaceTextureHelper?.stopListening()
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        peerConnection?.dispose()
        peerConnection = null
    }
}
