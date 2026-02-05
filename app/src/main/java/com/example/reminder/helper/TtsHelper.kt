package com.example.reminder.helper

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import com.example.reminder.data.preferences.TtsPreferences

object TtsHelper {

    private val MUSIC_STREAM_OEMS = setOf(
        "HONOR", "HUAWEI", "HISILICON", "XIAOMI", "REDMI", "POCO",
        "OPPO", "REALME", "VIVO", "ONEPLUS", "MEIZU"
    )

    /** true, если для TTS в режиме звонка лучше сразу использовать основной динамик (Honor, Xiaomi и др.). */
    fun useMusicStreamForCall(context: Context): Boolean {
        return Build.MANUFACTURER.uppercase() in MUSIC_STREAM_OEMS
    }
    fun createTts(context: Context, listener: TextToSpeech.OnInitListener): TextToSpeech {
        val engine = TtsPreferences.getSelectedEnginePackage(context)
        return if (engine != null) {
            TextToSpeech(context, listener, engine)
        } else {
            TextToSpeech(context, listener)
        }
    }

    fun setupCallAudioMode(context: Context): Int {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previousMode = am.mode
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            am.isSpeakerphoneOn = false
        }
        return previousMode
    }

    fun restoreAudioMode(context: Context, previousMode: Int) {
        if (previousMode >= 0) {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.mode = previousMode
            } catch (_: Exception) { }
        }
    }

    fun createVoiceCallParams(): Bundle {
        return Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL.toString())
        }
    }

    /** Параметры TTS в медиа-поток (динамик). Fallback, когда STREAM_VOICE_CALL не даёт звука (Honor, часть Xiaomi). */
    fun createMusicStreamParams(): Bundle {
        return Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC.toString())
        }
    }
}
