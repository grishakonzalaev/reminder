package com.example.reminder.ui.screens

import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.reminder.data.preferences.ReminderPreferences
import com.example.reminder.data.preferences.TtsPreferences
import com.example.reminder.helper.CalendarHelper
import com.example.reminder.scheduler.CalendarSyncScheduler
import java.util.Locale

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val engines: List<Pair<String, String?>> = remember { TtsPreferences.getAvailableEngines(ctx) }
    var selectedEngine by remember { mutableStateOf<String?>(TtsPreferences.getSelectedEnginePackage(ctx)) }
    var delaySeconds by remember { mutableStateOf(TtsPreferences.getSpeakDelaySeconds(ctx).toString()) }
    var speechRate by remember { mutableStateOf(TtsPreferences.getSpeechRate(ctx)) }
    var showTtsList by remember { mutableStateOf(false) }
    var testTts by remember { mutableStateOf<TextToSpeech?>(null) }
    var themeMode by remember { mutableStateOf(ReminderPreferences.getThemeMode(ctx)) }
    var addToCalendar by remember { mutableStateOf(ReminderPreferences.getAddToCalendar(ctx)) }
    var syncFromCalendar by remember { mutableStateOf(ReminderPreferences.getSyncFromCalendar(ctx)) }
    var writeCalendarId by remember { mutableStateOf(ReminderPreferences.getWriteCalendarId(ctx)) }
    var readCalendarId by remember { mutableStateOf(ReminderPreferences.getReadCalendarId(ctx)) }
    var showWriteCalendarDialog by remember { mutableStateOf(false) }
    var showReadCalendarDialog by remember { mutableStateOf(false) }
    var autoDeletePast by remember { mutableStateOf(ReminderPreferences.getAutoDeletePast(ctx)) }
    var useCallApi by remember { mutableStateOf(TtsPreferences.getUseCallApi(ctx)) }
    var snoozeEnabled by remember { mutableStateOf(ReminderPreferences.getSnoozeEnabled(ctx)) }
    var snoozeRepeats by remember { mutableStateOf(ReminderPreferences.getSnoozeRepeats(ctx).toString()) }
    var snoozeDelayMinutes by remember { mutableStateOf(ReminderPreferences.getSnoozeDelayMinutes(ctx).toString()) }
    val availableCalendars: List<Pair<Long, String>> = remember { CalendarHelper.getAvailableCalendars(ctx) }

    BackHandler(enabled = showTtsList || showWriteCalendarDialog || showReadCalendarDialog) {
        when {
            showTtsList -> showTtsList = false
            showWriteCalendarDialog -> showWriteCalendarDialog = false
            showReadCalendarDialog -> showReadCalendarDialog = false
        }
    }

    BackHandler(enabled = !showTtsList && !showWriteCalendarDialog && !showReadCalendarDialog) {
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            testTts?.shutdown()
            testTts = null
        }
    }

    val currentTtsLabel: String = remember(selectedEngine) {
        engines.find { (_: String, pkg: String?) -> pkg == selectedEngine }?.first ?: "По умолчанию"
    }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", modifier = Modifier.size(24.dp))
                    }
                    Text("Настройки", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(8.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Тема", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    ReminderPreferences.THEME_SYSTEM to "Как в системе",
                    ReminderPreferences.THEME_LIGHT to "Светлая",
                    ReminderPreferences.THEME_DARK to "Тёмная"
                ).forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                        onClick = {
                            themeMode = value
                            ReminderPreferences.setThemeMode(ctx, value)
                            (ctx as? android.app.Activity)?.recreate()
                        },
                        selected = themeMode == value,
                        label = { Text(label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Добавлять напоминания в календарь", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = addToCalendar,
                    onCheckedChange = {
                        addToCalendar = it
                        ReminderPreferences.setAddToCalendar(ctx, it)
                    }
                )
            }
            Text(
                "При включении каждое напоминание создаётся как событие в календаре Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Читать календарь и добавлять события как напоминания", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = syncFromCalendar,
                    onCheckedChange = {
                        syncFromCalendar = it
                        ReminderPreferences.setSyncFromCalendar(ctx, it)
                        if (it) CalendarSyncScheduler.scheduleIfEnabled(ctx) else CalendarSyncScheduler.cancel(ctx)
                    }
                )
            }
            Text(
                "При включении приложение раз в пол минуты читает календарь и добавляет будущие события (на 30 дней вперёд) в список напоминаний.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            val writeCalendarLabel = if (writeCalendarId == 0L) "Первый доступный" else availableCalendars.find { (id, _) -> id == writeCalendarId }?.second ?: "Календарь $writeCalendarId"
            val readCalendarLabel = if (readCalendarId == 0L) "Все календари" else availableCalendars.find { (id, _) -> id == readCalendarId }?.second ?: "Календарь $readCalendarId"
            Spacer(modifier = Modifier.height(16.dp))
            Text("Календарь для записи напоминаний", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showWriteCalendarDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(writeCalendarLabel)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Календарь для чтения событий", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showReadCalendarDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(readCalendarLabel)
            }
            if (showWriteCalendarDialog) {
                AlertDialog(
                    onDismissRequest = { showWriteCalendarDialog = false },
                    title = { Text("Календарь для записи") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        writeCalendarId = 0L
                                        ReminderPreferences.setWriteCalendarId(ctx, 0L)
                                        showWriteCalendarDialog = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Первый доступный")
                            }
                            for ((id, name) in availableCalendars) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            writeCalendarId = id
                                            ReminderPreferences.setWriteCalendarId(ctx, id)
                                            showWriteCalendarDialog = false
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showWriteCalendarDialog = false }) {
                            Text("Закрыть")
                        }
                    }
                )
            }
            if (showReadCalendarDialog) {
                AlertDialog(
                    onDismissRequest = { showReadCalendarDialog = false },
                    title = { Text("Календарь для чтения") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        readCalendarId = 0L
                                        ReminderPreferences.setReadCalendarId(ctx, 0L)
                                        showReadCalendarDialog = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Все календари")
                            }
                            for ((id, name) in availableCalendars) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            readCalendarId = id
                                            ReminderPreferences.setReadCalendarId(ctx, id)
                                            showReadCalendarDialog = false
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showReadCalendarDialog = false }) {
                            Text("Закрыть")
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Автоудаление прошедших напоминаний", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = autoDeletePast,
                    onCheckedChange = {
                        autoDeletePast = it
                        ReminderPreferences.setAutoDeletePast(ctx, it)
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Использовать API звонков", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = useCallApi,
                    onCheckedChange = {
                        useCallApi = it
                        TtsPreferences.setUseCallApi(ctx, it)
                    }
                )
            }
            Text(
                "Включено: напоминание приходит как звонок (интерфейс звонков), TTS — в разговорный динамик. Выключено: полноэкранное уведомление, TTS — в основной динамик.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Отложенные напоминания", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = snoozeEnabled,
                    onCheckedChange = {
                        snoozeEnabled = it
                        ReminderPreferences.setSnoozeEnabled(ctx, it)
                    }
                )
            }
            Text(
                "При отклонении звонка напоминание повторится через заданное время (до заданного числа повторов).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (snoozeEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Количество повторов", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = snoozeRepeats,
                    onValueChange = { s ->
                        if (s.isEmpty() || s.all { it.isDigit() }) {
                            snoozeRepeats = s
                            s.toIntOrNull()?.coerceIn(0, 10)?.let { ReminderPreferences.setSnoozeRepeats(ctx, it) }
                        }
                    },
                    label = { Text("0–10 (сколько раз повторить после отложения)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("На сколько минут откладывать", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = snoozeDelayMinutes,
                    onValueChange = { s ->
                        if (s.isEmpty() || s.all { it.isDigit() }) {
                            snoozeDelayMinutes = s
                            s.toIntOrNull()?.coerceIn(1, 60)?.let { ReminderPreferences.setSnoozeDelayMinutes(ctx, it) }
                        }
                    },
                    label = { Text("1–60 минут") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Синтезатор речи", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showTtsList = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(currentTtsLabel)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Задержка озвучивания (сек)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = delaySeconds,
                onValueChange = { new ->
                    if (new.isEmpty() || new.all { it.isDigit() }) {
                        delaySeconds = new
                        new.toIntOrNull()?.let { TtsPreferences.setSpeakDelaySeconds(ctx, it.coerceIn(TtsPreferences.MIN_SPEAK_DELAY, TtsPreferences.MAX_SPEAK_DELAY)) }
                    }
                },
                label = { Text("Секунд после ответа (0–120)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            val parsed = delaySeconds.toIntOrNull() ?: TtsPreferences.DEFAULT_SPEAK_DELAY_SECONDS
            if (delaySeconds.isNotEmpty() && (parsed < TtsPreferences.MIN_SPEAK_DELAY || parsed > TtsPreferences.MAX_SPEAK_DELAY)) {
                Text(
                    "Укажите число от ${TtsPreferences.MIN_SPEAK_DELAY} до ${TtsPreferences.MAX_SPEAK_DELAY}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Скорость речи", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = speechRate,
                    onValueChange = { v ->
                        speechRate = v
                        TtsPreferences.setSpeechRate(ctx, v)
                    },
                    valueRange = TtsPreferences.MIN_SPEECH_RATE..TtsPreferences.MAX_SPEECH_RATE,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "%.1f".format(Locale.US, speechRate),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (testTts != null) return@Button
                    val engine = selectedEngine
                    val listener = TextToSpeech.OnInitListener { status: Int ->
                            if (status == TextToSpeech.SUCCESS) {
                            val t = testTts ?: return@OnInitListener
                            t.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                                override fun onStart(utteranceId: String?) {}
                                override fun onDone(utteranceId: String?) {
                                    (ctx as? android.app.Activity)?.runOnUiThread {
                                        t.shutdown()
                                        testTts = null
                                    }
                                }
                                override fun onError(utteranceId: String?) {
                                    (ctx as? android.app.Activity)?.runOnUiThread {
                                        t.shutdown()
                                        testTts = null
                                    }
                                }
                            })
                            t.setSpeechRate(TtsPreferences.getSpeechRate(ctx))
                            t.language = Locale.getDefault()
                            t.speak("Проверка синтеза речи.", TextToSpeech.QUEUE_FLUSH, null, "test_done")
                        } else {
                            (ctx as? android.app.Activity)?.runOnUiThread {
                                testTts?.shutdown()
                                testTts = null
                            }
                        }
                    }
                    val tts = if (engine != null) {
                        TextToSpeech(ctx, listener, engine)
                    } else {
                        TextToSpeech(ctx, listener)
                    }
                    testTts = tts
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = testTts == null
            ) {
                Text(if (testTts != null) "Озвучивание…" else "Тест")
            }
        }
    }
    if (showTtsList) {
        AlertDialog(
            onDismissRequest = { showTtsList = false },
            title = { Text("Синтезатор речи") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    engines.forEach { (label, pkg) ->
                        val selected = (pkg == null && selectedEngine == null) || (pkg != null && pkg == selectedEngine)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    TtsPreferences.setSelectedEnginePackage(ctx, pkg)
                                    selectedEngine = pkg
                                    showTtsList = false
                                },
                            shape = MaterialTheme.shapes.small,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTtsList = false }) { Text("Закрыть") }
            }
        )
    }
}
