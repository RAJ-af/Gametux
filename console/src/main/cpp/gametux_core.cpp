#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#define TAG "GametuxCore"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_gametux_console_emulator_EmulatorCore_stringFromJNI(JNIEnv* env, jobject /* this */) {
    std::string hello = "Gametux Core Initialized";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_gametux_console_emulator_EmulatorCore_renderFrame(JNIEnv* env, jobject /* this */, jobject surface) {
    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    if (window == nullptr) return;

    ANativeWindow_setBuffersGeometry(window, 256, 240, WINDOW_FORMAT_RGBA_8888);
    ANativeWindow_Buffer buffer;
    if (ANativeWindow_lock(window, &buffer, nullptr) == 0) {
        // Fill with a simple pattern for MVP
        uint32_t* pixels = (uint32_t*)buffer.bits;
        static int offset = 0;
        for (int y = 0; y < buffer.height; y++) {
            for (int x = 0; x < buffer.width; x++) {
                pixels[y * buffer.stride + x] = (x + offset) % 256 | ((y + offset) % 256 << 8) | (0xFF << 24);
            }
        }
        offset++;
        ANativeWindow_unlockAndPost(window);
    }
    ANativeWindow_release(window);
}
