package com.example.reminder

import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class CallActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private var ringtone: Ringtone? = null
    private var message: String? = null
    private var pendingSpeak = false
    /** true после вызова onInit — только тогда можно вызывать speak() */
    private var ttsReady = false
    private var previousAudioMode = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContentView(R.layout.activity_call)

        message = intent.getStringExtra(EXTRA_MSG) ?: "Пора!"
        val answeredByHeadset = intent.getBooleanExtra(EXTRA_ANSWERED_BY_HEADSET, false)

        findViewById<TextView>(R.id.callerName).text = "Напоминание"
        findViewById<TextView>(R.id.callMessage).text = message
        findViewById<TextView>(R.id.callMessage).visibility = android.view.View.VISIBLE

        // Если ответили с гарнитуры — не звоним и не вибрируем, через 5 сек TTS
        if (answeredByHeadset) {
            tts = createTts()
            scheduleSpeak() // озвучить через 5 сек после открытия экрана
            findViewById<Button>(R.id.btnAccept).setOnClickListener { scheduleSpeak() }
            findViewById<Button>(R.id.btnDecline).setOnClickListener { finish() }
            return
        }

        // Вибрация
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), intArrayOf(0, 128, 0), 1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 500, 500), 1)
            }
        }

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone?.play()

        tts = createTts()

        findViewById<Button>(R.id.btnAccept).setOnClickListener {
            ringtone?.stop()
            vibrator?.cancel()
            scheduleSpeak()
        }

        findViewById<Button>(R.id.btnDecline).setOnClickListener {
            ringtone?.stop()
            vibrator?.cancel()
            finish()
        }
    }

    private fun createTts(): TextToSpeech {
        val engine = TtsPreferences.getSelectedEnginePackage(this)
        return if (engine != null) {
            TextToSpeech(this, this, engine)
        } else {
            TextToSpeech(this, this)
        }
    }

    /** Озвучить напоминание через N секунд после ответа (N — из настроек) */
    private fun scheduleSpeak() {
        val delayMs = (TtsPreferences.getSpeakDelaySeconds(this).coerceIn(TtsPreferences.MIN_SPEAK_DELAY, TtsPreferences.MAX_SPEAK_DELAY) * 1000L)
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isDestroyed && ::tts.isInitialized) {
                if (ttsReady) {
                    speakAndFinish()
                } else {
                    pendingSpeak = true
                }
            }
        }, delayMs)
    }

    private fun speakAndFinish() {
        if (isDestroyed || !::tts.isInitialized || !ttsReady) return
        val text = message ?: return
        tts.language = Locale.getDefault()
        tts.setSpeechRate(TtsPreferences.getSpeechRate(this))
        val useEarpiece = TtsPreferences.getUseCallApi(this)
        val params = if (useEarpiece) {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            previousAudioMode = am.mode
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            am.isSpeakerphoneOn = false
            Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL.toString())
            }
        } else {
            null
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_DONE)
    }

    private fun restoreAudioMode() {
        if (previousAudioMode >= 0) {
            try {
                val am = getSystemService(AUDIO_SERVICE) as AudioManager
                am.mode = previousAudioMode
            } catch (_: Exception) { }
            previousAudioMode = -1
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && !isDestroyed) {
            ttsReady = true
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    runOnUiThread {
                        restoreAudioMode()
                        if (utteranceId == UTTERANCE_DONE) finish()
                    }
                }
                override fun onError(utteranceId: String?) {
                    runOnUiThread {
                        restoreAudioMode()
                        finish()
                    }
                }
            })
            if (pendingSpeak) {
                pendingSpeak = false
                speakAndFinish()
            }
        }
    }

    override fun onDestroy() {
        restoreAudioMode()
        if (::tts.isInitialized) tts.shutdown()
        ringtone?.stop()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MSG = "MSG"
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_ANSWERED_BY_HEADSET = "answered_by_headset"
        private const val UTTERANCE_DONE = "utterance_done"
    }
}