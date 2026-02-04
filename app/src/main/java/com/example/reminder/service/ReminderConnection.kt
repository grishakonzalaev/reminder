package com.example.reminder.service

import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.telecom.Connection
import androidx.annotation.RequiresApi
import com.example.reminder.app.ReminderApp
import com.example.reminder.data.preferences.TtsPreferences
import com.example.reminder.helper.SnoozeHelper
import com.example.reminder.receiver.ReminderReceiver
import java.util.Locale

/**
 * Представляет «звонок» напоминания в системе Telecom.
 * При ответе остаётся стандартный экран звонка Android, TTS воспроизводится как звук звонка (разговорный динамик).
 */
@RequiresApi(Build.VERSION_CODES.M)
class ReminderConnection : Connection() {

    private var tts: TextToSpeech? = null
    private var previousAudioMode = -1
    private val mainHandler = Handler(Looper.getMainLooper())
    private var delayedSpeakRunnable: Runnable? = null

    override fun onAnswer() {
        setActive()
        val ctx = ReminderApp.instance ?: return
        val extras = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> getExtras()
            else -> null
        } ?: ReminderConnectionService.lastRequestExtras
        val msg = extras?.getString(ReminderReceiver.EXTRA_MSG) ?: "Пора!"
        val delayMs = (TtsPreferences.getSpeakDelaySeconds(ctx).coerceIn(TtsPreferences.MIN_SPEAK_DELAY, TtsPreferences.MAX_SPEAK_DELAY) * 1000L)
        val engine = TtsPreferences.getSelectedEnginePackage(ctx)

        delayedSpeakRunnable = Runnable {
            delayedSpeakRunnable = null
            if (getState() != Connection.STATE_DISCONNECTED) {
                startTtsAsCall(ctx, msg, engine)
            }
        }
        mainHandler.postDelayed(delayedSpeakRunnable!!, delayMs)
    }

    private fun startTtsAsCall(ctx: android.content.Context, message: String, engine: String?) {
        val am = ctx.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
        previousAudioMode = am.mode
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        am.isSpeakerphoneOn = false

        val connection = this
        val initListener = TextToSpeech.OnInitListener { status: Int ->
            if (status == TextToSpeech.SUCCESS) {
                val ttsRef = tts ?: return@OnInitListener
                ttsRef.setSpeechRate(TtsPreferences.getSpeechRate(ctx))
                ttsRef.language = Locale.getDefault()
                ttsRef.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        mainHandler.post {
                            connection.finishCallAndTts(am)
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        mainHandler.post {
                            connection.finishCallAndTts(am)
                        }
                    }
                })
                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL.toString())
                }
                ttsRef.speak(message, TextToSpeech.QUEUE_FLUSH, params, "reminder_done")
            } else {
                mainHandler.post {
                    connection.finishCallAndTts(am)
                }
            }
        }
        tts = if (engine != null) {
            TextToSpeech(ctx, initListener, engine)
        } else {
            TextToSpeech(ctx, initListener)
        }
    }

    private fun finishCallAndTts(am: AudioManager) {
        if (previousAudioMode >= 0) {
            try {
                am.mode = previousAudioMode
            } catch (_: Exception) { }
            previousAudioMode = -1
        }
        tts?.shutdown()
        tts = null
        destroy()
    }

    override fun onReject() {
        delayedSpeakRunnable?.let { mainHandler.removeCallbacks(it) }
        delayedSpeakRunnable = null
        val ctx = ReminderApp.instance ?: run { destroy(); return }
        val extras = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> getExtras()
            else -> null
        } ?: ReminderConnectionService.lastRequestExtras
        val id = extras?.getLong(ReminderReceiver.EXTRA_ID, -1L) ?: -1L
        val msg = extras?.getString(ReminderReceiver.EXTRA_MSG) ?: "Пора!"
        if (id >= 0) SnoozeHelper.tryScheduleSnooze(ctx, id, msg)
        destroy()
    }

    override fun onAbort() {
        delayedSpeakRunnable?.let { mainHandler.removeCallbacks(it) }
        delayedSpeakRunnable = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        destroy()
    }

    override fun onDisconnect() {
        delayedSpeakRunnable?.let { mainHandler.removeCallbacks(it) }
        delayedSpeakRunnable = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        destroy()
    }
}
