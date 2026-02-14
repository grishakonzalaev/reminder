package com.example.reminder.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reminder.R
import com.example.reminder.data.model.Reminder
import com.example.reminder.ui.components.DateTimePickerSliders
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val REPEAT_VALUES = listOf("none", "daily", "monthly", "yearly")

@Composable
fun ReminderDialog(
    reminder: Reminder? = null,
    onDismiss: () -> Unit,
    onConfirm: (message: String, timeMillis: Long, repeatType: String) -> Unit
) {
    val isEdit = reminder != null
    var message by remember(reminder?.id) { mutableStateOf(reminder?.message ?: "") }
    var timeMillis by remember(reminder?.id) { mutableStateOf(reminder?.timeMillis ?: System.currentTimeMillis() + 60_000) }
    var repeatType by remember(reminder?.id) { mutableStateOf(reminder?.repeatType.takeIf { !it.isNullOrBlank() } ?: "none") }

    val formattedTime = remember(timeMillis) {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timeMillis))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isEdit) R.string.dialog_edit_reminder else R.string.dialog_new_reminder)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.dialog_repeat), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    REPEAT_VALUES.forEach { value ->
                        val label = when (value) {
                            "none" -> stringResource(R.string.repeat_none)
                            "daily" -> stringResource(R.string.repeat_daily)
                            "monthly" -> stringResource(R.string.repeat_monthly)
                            "yearly" -> stringResource(R.string.repeat_yearly)
                            else -> value
                        }
                        FilterChip(
                            selected = repeatType == value,
                            onClick = { repeatType = value },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(stringResource(R.string.dialog_reminder_text)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(16.dp))
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
                        onConfirm(message, timeMillis, repeatType)
                    }
                }
            ) {
                Text(stringResource(R.string.button_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}
