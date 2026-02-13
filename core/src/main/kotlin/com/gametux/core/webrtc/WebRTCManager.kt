package com.gametux.core.webrtc

import android.content.Context
import org.webrtc.*

class WebRTCManager(private val context: Context) {
    private val factory: PeerConnectionFactory

    init {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
            null, // EGL Context can be passed here if using GL textures
            true, // enableIntelVp8Encoder
            true  // enableH264HighProfile
        )
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(null)

        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(
        rtcConfig: PeerConnection.RTCConfiguration,
        observer: PeerConnection.Observer
    ): PeerConnection? {
        return factory.createPeerConnection(rtcConfig, observer)
    }

    fun createVideoSource(isScreencast: Boolean): VideoSource {
        return factory.createVideoSource(isScreencast)
    }

    fun createVideoTrack(id: String, source: VideoSource): VideoTrack {
        return factory.createVideoTrack(id, source)
    }

    fun createAudioSource(constraints: MediaConstraints): AudioSource {
        return factory.createAudioSource(constraints)
    }

    fun createAudioTrack(id: String, source: AudioSource): AudioTrack {
        return factory.createAudioTrack(id, source)
    }

    fun dispose() {
        factory.dispose()
    }
}
