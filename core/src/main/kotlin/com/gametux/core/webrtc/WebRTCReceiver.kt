package com.gametux.core.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRTCReceiver(
    private val context: Context,
    private val manager: WebRTCManager,
    private val onVideoTrack: (VideoTrack) -> Unit
) {
    private val TAG = "WebRTCReceiver"
    private var peerConnection: PeerConnection? = null

    fun handleOffer(offerSdp: String, onAnswerCreated: (String) -> Unit, onIceCandidate: (String) -> Unit) {
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = manager.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                onIceCandidate(candidate.sdp)
            }
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: $newState")
            }
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
                val track = receiver.track()
                if (track is VideoTrack) {
                    onVideoTrack(track)
                }
            }
            override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(dataChannel: DataChannel) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onRenegotiationNeeded() {}
        })

        val offer = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {}
            override fun onSetSuccess() {
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription) {
                        peerConnection?.setLocalDescription(this, sdp)
                        onAnswerCreated(sdp.description)
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(error: String) {}
                    override fun onSetFailure(error: String) {}
                }, MediaConstraints())
            }
            override fun onCreateFailure(error: String) {}
            override fun onSetFailure(error: String) {}
        }, offer)
    }

    fun addIceCandidate(candidateSdp: String) {
        val candidate = IceCandidate("0", 0, candidateSdp)
        peerConnection?.addIceCandidate(candidate)
    }

    fun stop() {
        peerConnection?.dispose()
        peerConnection = null
    }
}
