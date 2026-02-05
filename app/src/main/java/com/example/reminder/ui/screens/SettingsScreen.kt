package com.example.reminder.ui.screens

import android.app.Activity
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.reminder.R
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
    var language by remember { mutableStateOf(ReminderPreferences.getLanguage(ctx)) }
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
        engines.find { (_: String, pkg: String?) -> pkg == selectedEngine }?.first ?: ctx.getString(R.string.settings_tts_default)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back), modifier = Modifier.size(24.dp))
                    }
                    Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(8.dp))
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
            Text(ctx.getString(com.example.reminder.R.string.settings_language), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    ReminderPreferences.LANG_SYSTEM to ctx.getString(com.example.reminder.R.string.language_system),
                    ReminderPreferences.LANG_ENGLISH to Locale("en").getDisplayName(Locale("en")).replaceFirstChar { it.uppercase() },
                    ReminderPreferences.LANG_RUSSIAN to Locale("ru").getDisplayName(Locale("ru")).replaceFirstChar { it.uppercase() }
                ).forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                        onClick = {
                            language = value
                            ReminderPreferences.setLanguage(ctx, value)

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                ReminderPreferences.applyLanguage(ctx)
                                (ctx as? Activity)?.recreate()
                            } else {
                                ReminderPreferences.applyLanguageForSystemSync(ctx)
                            }
                        },
                        selected = language == value,
                        label = { Text(label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(ctx.getString(com.example.reminder.R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    ReminderPreferences.THEME_SYSTEM to ctx.getString(com.example.reminder.R.string.theme_system),
                    ReminderPreferences.THEME_LIGHT to ctx.getString(com.example.reminder.R.string.theme_light),
                    ReminderPreferences.THEME_DARK to ctx.getString(com.example.reminder.R.string.theme_dark)
                ).forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                        onClick = {
                            themeMode = value
                            ReminderPreferences.setThemeMode(ctx, value)
                            (ctx as? Activity)?.recreate()
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
                Text(stringResource(R.string.settings_add_to_calendar), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = addToCalendar,
                    onCheckedChange = {
                        addToCalendar = it
                        ReminderPreferences.setAddToCalendar(ctx, it)
                    }
                )
            }
            Text(
                stringResource(R.string.settings_add_to_calendar_description),
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
                Text(stringResource(R.string.settings_sync_from_calendar), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
                stringResource(R.string.settings_sync_from_calendar_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            val writeCalendarLabel = if (writeCalendarId == 0L) ctx.getString(R.string.calendar_first_available) else availableCalendars.find { (id, _) -> id == writeCalendarId }?.second ?: ctx.getString(R.string.calendar_id, writeCalendarId)
            val readCalendarLabel = if (readCalendarId == 0L) ctx.getString(R.string.calendar_all) else availableCalendars.find { (id, _) -> id == readCalendarId }?.second ?: ctx.getString(R.string.calendar_id, readCalendarId)
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.settings_write_calendar), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showWriteCalendarDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(writeCalendarLabel)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.settings_read_calendar), style = MaterialTheme.typography.titleMedium)
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
                    title = { Text(stringResource(R.string.dialog_write_calendar)) },
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
                                Text(stringResource(R.string.calendar_first_available))
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
                            Text(stringResource(R.string.button_close))
                        }
                    }
                )
            }
            if (showReadCalendarDialog) {
                AlertDialog(
                    onDismissRequest = { showReadCalendarDialog = false },
                    title = { Text(stringResource(R.string.dialog_read_calendar)) },
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
                                Text(stringResource(R.string.calendar_all))
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
                            Text(stringResource(R.string.button_close))
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
                Text(stringResource(R.string.settings_auto_delete), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
                Text(stringResource(R.string.settings_use_call_api), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = useCallApi,
                    onCheckedChange = {
                        useCallApi = it
                        TtsPreferences.setUseCallApi(ctx, it)
                    }
                )
            }
            Text(
                stringResource(R.string.settings_use_call_api_description),
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
                Text(stringResource(R.string.settings_snooze), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = snoozeEnabled,
                    onCheckedChange = {
                        snoozeEnabled = it
                        ReminderPreferences.setSnoozeEnabled(ctx, it)
                    }
                )
            }
            Text(
                stringResource(R.string.settings_snooze_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (snoozeEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.settings_snooze_repeats), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = snoozeRepeats,
                    onValueChange = { s ->
                        if (s.isEmpty() || s.all { it.isDigit() }) {
                            snoozeRepeats = s
                            s.toIntOrNull()?.coerceIn(0, 10)?.let { ReminderPreferences.setSnoozeRepeats(ctx, it) }
                        }
                    },
                    label = { Text(stringResource(R.string.settings_snooze_repeats_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.settings_snooze_delay), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = snoozeDelayMinutes,
                    onValueChange = { s ->
                        if (s.isEmpty() || s.all { it.isDigit() }) {
                            snoozeDelayMinutes = s
                            s.toIntOrNull()?.coerceIn(1, 60)?.let { ReminderPreferences.setSnoozeDelayMinutes(ctx, it) }
                        }
                    },
                    label = { Text(stringResource(R.string.settings_snooze_delay_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.settings_tts), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showTtsList = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(currentTtsLabel)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.settings_tts_delay), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = delaySeconds,
                onValueChange = { new ->
                    if (new.isEmpty() || new.all { it.isDigit() }) {
                        delaySeconds = new
                        new.toIntOrNull()?.let { TtsPreferences.setSpeakDelaySeconds(ctx, it.coerceIn(TtsPreferences.MIN_SPEAK_DELAY, TtsPreferences.MAX_SPEAK_DELAY)) }
                    }
                },
                label = { Text(stringResource(R.string.settings_tts_delay_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            val parsed = delaySeconds.toIntOrNull() ?: TtsPreferences.DEFAULT_SPEAK_DELAY_SECONDS
            if (delaySeconds.isNotEmpty() && (parsed < TtsPreferences.MIN_SPEAK_DELAY || parsed > TtsPreferences.MAX_SPEAK_DELAY)) {
                Text(
                    ctx.getString(R.string.settings_tts_delay_error, TtsPreferences.MIN_SPEAK_DELAY, TtsPreferences.MAX_SPEAK_DELAY),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.settings_speech_rate), style = MaterialTheme.typography.titleMedium)
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
                            t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                override fun onStart(utteranceId: String?) {}
                                override fun onDone(utteranceId: String?) {
                                    (ctx as? Activity)?.runOnUiThread {
                                        t.shutdown()
                                        testTts = null
                                    }
                                }
                                override fun onError(utteranceId: String?) {
                                    (ctx as? Activity)?.runOnUiThread {
                                        t.shutdown()
                                        testTts = null
                                    }
                                }
                            })
                            t.setSpeechRate(TtsPreferences.getSpeechRate(ctx))
                            t.language = Locale.getDefault()
                            t.speak(ctx.getString(R.string.tts_test_message), TextToSpeech.QUEUE_FLUSH, null, "test_done")
                        } else {
                            (ctx as? Activity)?.runOnUiThread {
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
                Text(if (testTts != null) stringResource(R.string.button_testing) else stringResource(R.string.button_test))
            }
        }
    }
    if (showTtsList) {
        AlertDialog(
            onDismissRequest = { showTtsList = false },
            title = { Text(stringResource(R.string.dialog_tts_title)) },
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
                TextButton(onClick = { showTtsList = false }) { Text(stringResource(R.string.button_close)) }
            }
        )
    }
}
