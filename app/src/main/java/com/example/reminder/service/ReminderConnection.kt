package com.example.reminder.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
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
 * На части устройств (Honor, Xiaomi) STREAM_VOICE_CALL может не давать звука — тогда используется fallback в STREAM_MUSIC.
 */
@RequiresApi(Build.VERSION_CODES.M)
class ReminderConnection : Connection() {

    private var tts: TextToSpeech? = null
    private var previousAudioMode = -1
    private val mainHandler = Handler(Looper.getMainLooper())
    private var delayedSpeakRunnable: Runnable? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusListener: AudioManager.OnAudioFocusChangeListener? = null
    private var ttsFinished = false
    private var fallbackTimeoutRunnable: Runnable? = null

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
        requestCallAudioFocus(ctx)

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
                            connection.onTtsDone(ctx)
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        mainHandler.post {
                            connection.onTtsDone(ctx)
                        }
                    }
                })
                val params = if (TtsHelper.useMusicStreamForCall(ctx)) {
                    TtsHelper.createMusicStreamParams()
                } else {
                    TtsHelper.createVoiceCallParams()
                }
                ttsRef.speak(message, TextToSpeech.QUEUE_FLUSH, params, "reminder_done")
                scheduleFallbackIfSilent(ctx, message)
            } else {
                mainHandler.post {
                    connection.finishCallAndTts(ctx)
                }
            }
        }
        tts = TtsHelper.createTts(ctx, initListener)
    }

    private fun requestCallAudioFocus(ctx: Context) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .build()
            audioFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            val listener = AudioManager.OnAudioFocusChangeListener { }
            audioFocusListener = listener
            @Suppress("DEPRECATION")
            am.requestAudioFocus(listener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun scheduleFallbackIfSilent(ctx: Context, message: String) {
        fallbackTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        fallbackTimeoutRunnable = Runnable {
            fallbackTimeoutRunnable = null
            if (ttsFinished || getState() == Connection.STATE_DISCONNECTED) return@Runnable
            tryFallbackSpeak(ctx, message)
        }
        mainHandler.postDelayed(fallbackTimeoutRunnable!!, 4000L)
    }

    private fun tryFallbackSpeak(ctx: Context, message: String) {
        if (ttsFinished || getState() == Connection.STATE_DISCONNECTED) return
        val ttsRef = tts ?: return
        val connection = this
        ttsRef.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                mainHandler.post { connection.onTtsDone(ctx) }
            }
            override fun onError(utteranceId: String?) {
                mainHandler.post { connection.onTtsDone(ctx) }
            }
        })
        val params = TtsHelper.createMusicStreamParams()
        ttsRef.speak(message, TextToSpeech.QUEUE_FLUSH, params, "reminder_fallback_done")
        mainHandler.postDelayed({
            if (!ttsFinished && getState() != Connection.STATE_DISCONNECTED) {
                connection.onTtsDone(ctx)
            }
        }, 30000L)
    }

    private fun onTtsDone(ctx: Context) {
        if (ttsFinished) return
        ttsFinished = true
        fallbackTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        fallbackTimeoutRunnable = null
        finishCallAndTts(ctx)
    }

    private fun finishCallAndTts(ctx: Context) {
        fallbackTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        fallbackTimeoutRunnable = null
        abandonCallAudioFocus(ctx)
        TtsHelper.restoreAudioMode(ctx, previousAudioMode)
        previousAudioMode = -1
        tts?.shutdown()
        tts = null
        destroy()
    }

    private fun abandonCallAudioFocus(ctx: Context) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            audioFocusListener?.let {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(it)
            }
            audioFocusListener = null
        }
    }

    override fun onReject() {
        delayedSpeakRunnable?.let { mainHandler.removeCallbacks(it) }
        delayedSpeakRunnable = null
        fallbackTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        fallbackTimeoutRunnable = null
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
        fallbackTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        fallbackTimeoutRunnable = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        destroy()
    }

    override fun onDisconnect() {
        delayedSpeakRunnable?.let { mainHandler.removeCallbacks(it) }
        delayedSpeakRunnable = null
        fallbackTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        fallbackTimeoutRunnable = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        destroy()
    }
}
