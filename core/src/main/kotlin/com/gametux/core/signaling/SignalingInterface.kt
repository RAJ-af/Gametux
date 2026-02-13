package com.gametux.core.signaling

interface SignalingInterface {
    fun sendOffer(sdp: String, onAnswer: (String) -> Unit)
    fun sendIceCandidate(candidate: String)
    fun onIceCandidateReceived(callback: (String) -> Unit)
}
