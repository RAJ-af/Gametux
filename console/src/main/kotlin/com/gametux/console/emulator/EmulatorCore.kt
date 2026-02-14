package com.gametux.console.emulator

import android.view.Surface

import android.util.Log

class EmulatorCore {
    companion object {
        private const val TAG = "EmulatorCore"

        init {
            try {
                // TODO: Replace prebuilt .so with local NDK build later
                System.loadLibrary("gametux_core")
                Log.d(TAG, "Native library gametux_core loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library gametux_core: ${e.message}")
            }
        }
    }

    external fun stringFromJNI(): String
    external fun renderFrame(surface: Surface)
}
