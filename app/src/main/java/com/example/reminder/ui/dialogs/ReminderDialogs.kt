package com.example.reminder.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.reminder.data.model.Reminder
import com.example.reminder.ui.components.DateTimePickerSliders
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReminderDialog(
    reminder: Reminder? = null,
    onDismiss: () -> Unit,
    onConfirm: (message: String, timeMillis: Long) -> Unit
) {
    val isEdit = reminder != null
    var message by remember(reminder?.id) { mutableStateOf(reminder?.message ?: "") }
    var timeMillis by remember(reminder?.id) { mutableStateOf(reminder?.timeMillis ?: System.currentTimeMillis() + 60_000) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Редактировать напоминание" else "Новое напоминание") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Текст напоминания (озвучит TTS)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Время: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timeMillis))}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                DateTimePickerSliders(
                    timeMillis = timeMillis,
                    onTimeChanged = { timeMillis = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (message.isNotBlank()) {
                        onConfirm(message, timeMillis)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
