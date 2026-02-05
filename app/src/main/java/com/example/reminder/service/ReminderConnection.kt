package com.example.reminder.service

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.telecom.Connection
import androidx.annotation.RequiresApi
import com.example.reminder.Constants
import com.example.reminder.app.ReminderApp
import com.example.reminder.data.preferences.TtsPreferences
import com.example.reminder.helper.SnoozeHelper
import com.example.reminder.helper.TtsHelper
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
        val msg = extras?.getString(ReminderReceiver.EXTRA_MSG) ?: Constants.DEFAULT_REMINDER_MESSAGE
        val delayMs = (TtsPreferences.getSpeakDelaySeconds(ctx).coerceIn(TtsPreferences.MIN_SPEAK_DELAY, TtsPreferences.MAX_SPEAK_DELAY) * 1000L)

        delayedSpeakRunnable = Runnable {
            delayedSpeakRunnable = null
            if (getState() != Connection.STATE_DISCONNECTED) {
                startTtsAsCall(ctx, msg)
            }
        }
        mainHandler.postDelayed(delayedSpeakRunnable!!, delayMs)
    }

    private fun startTtsAsCall(ctx: Context, message: String) {
        previousAudioMode = TtsHelper.setupCallAudioMode(ctx)

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
                            connection.finishCallAndTts(ctx)
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        mainHandler.post {
                            connection.finishCallAndTts(ctx)
                        }
                    }
                })
                val params = TtsHelper.createVoiceCallParams()
                ttsRef.speak(message, TextToSpeech.QUEUE_FLUSH, params, "reminder_done")
            } else {
                mainHandler.post {
                    connection.finishCallAndTts(ctx)
                }
            }
        }
        tts = TtsHelper.createTts(ctx, initListener)
    }

    private fun finishCallAndTts(ctx: Context) {
        TtsHelper.restoreAudioMode(ctx, previousAudioMode)
        previousAudioMode = -1
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
        val msg = extras?.getString(ReminderReceiver.EXTRA_MSG) ?: Constants.DEFAULT_REMINDER_MESSAGE
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
