package com.gametux.console.emulator

import android.view.Surface

class EmulatorCore {
    companion object {
        init {
            System.loadLibrary("gametux_core")
        }
    }

    external fun stringFromJNI(): String
    external fun renderFrame(surface: Surface)
}
