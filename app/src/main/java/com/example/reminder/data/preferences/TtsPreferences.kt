package com.example.reminder.data.preferences

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.speech.tts.TextToSpeech

object TtsPreferences {
    private const val PREFS = "tts_prefs"
    private const val KEY_ENGINE = "engine_package"
    private const val KEY_SPEAK_DELAY_SECONDS = "speak_delay_seconds"
    private const val KEY_SPEECH_RATE = "speech_rate"
    private const val KEY_USE_CALL_API = "use_call_api"
    const val DEFAULT_SPEAK_DELAY_SECONDS = 5
    const val DEFAULT_USE_CALL_API = true
    const val DEFAULT_SPEECH_RATE = 1f
    const val MIN_SPEECH_RATE = 0.5f
    const val MAX_SPEECH_RATE = 2f
    const val MIN_SPEAK_DELAY = 0
    const val MAX_SPEAK_DELAY = 120

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getSpeakDelaySeconds(context: Context): Int {
        val v = prefs(context).getInt(KEY_SPEAK_DELAY_SECONDS, DEFAULT_SPEAK_DELAY_SECONDS)
        return v.coerceIn(MIN_SPEAK_DELAY, MAX_SPEAK_DELAY)
    }

    fun setSpeakDelaySeconds(context: Context, seconds: Int) {
        prefs(context).edit()
            .putInt(KEY_SPEAK_DELAY_SECONDS, seconds.coerceIn(MIN_SPEAK_DELAY, MAX_SPEAK_DELAY)).apply()
    }

    fun getSpeechRate(context: Context): Float {
        val v = prefs(context).getFloat(KEY_SPEECH_RATE, DEFAULT_SPEECH_RATE)
        return v.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
    }

    fun setSpeechRate(context: Context, rate: Float) {
        prefs(context).edit()
            .putFloat(KEY_SPEECH_RATE, rate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)).apply()
    }

    /** Включено: напоминание доставляется через API звонков (Telecom), TTS в разговорный динамик. */
    fun getUseCallApi(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_USE_CALL_API, DEFAULT_USE_CALL_API)
    }

    fun setUseCallApi(context: Context, use: Boolean) {
        prefs(context).edit().putBoolean(KEY_USE_CALL_API, use).apply()
    }

    fun getSelectedEnginePackage(context: Context): String? {
        val pkg = prefs(context).getString(KEY_ENGINE, null)
        return pkg?.takeIf { it.isNotBlank() }
    }

    fun setSelectedEnginePackage(context: Context, packageName: String?) {
        prefs(context).edit().putString(KEY_ENGINE, packageName ?: "").apply()
    }

    /**
     * Список установленных движков синтеза речи: (название, packageName).
     * Первый элемент — "По умолчанию" с packageName = null.
     * TTS-движки регистрируются как сервисы (Service), не активности.
     */
    fun getAvailableEngines(context: Context): List<Pair<String, String?>> {
        val list = mutableListOf<Pair<String, String?>>()
        list.add("По умолчанию" to null)
        val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        @Suppress("DEPRECATION")
        val resolveList: List<ResolveInfo> = context.packageManager.queryIntentServices(intent, 0)
        for (ri in resolveList) {
            val pkg = ri.serviceInfo.packageName
            val label = ri.loadLabel(context.packageManager).toString().ifBlank { pkg }
            list.add(label to pkg)
        }
        return list
    }
}
