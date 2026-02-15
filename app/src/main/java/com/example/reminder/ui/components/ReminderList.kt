package com.example.reminder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reminder.R
import com.example.reminder.data.model.Reminder

private fun Reminder.isRepeating(): Boolean {
    val t = repeatType ?: return false
    return t != "none"
}

@Composable
fun ReminderList(
    reminders: List<Reminder>,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onEdit: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    val repeating = reminders.filter { it.isRepeating() }
    val oneTime = reminders.filter { !it.isRepeating() }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (repeating.isNotEmpty()) {
            item(key = "header_repeating") {
                Text(
                    text = stringResource(R.string.section_repeating),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(repeating, key = { it.id }) { reminder ->
                ReminderItem(
                    reminder = reminder,
                    selectionMode = selectionMode,
                    isSelected = reminder.id in selectedIds,
                    onToggleSelect = { onToggleSelect(reminder.id) },
                    onEdit = { onEdit(reminder) },
                    onDelete = { onDelete(reminder) }
                )
            }
            item(key = "spacer_after_repeating") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        if (oneTime.isNotEmpty()) {
            item(key = "header_one_time") {
                Text(
                    text = stringResource(R.string.section_one_time),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(oneTime, key = { it.id }) { reminder ->
                ReminderItem(
                    reminder = reminder,
                    selectionMode = selectionMode,
                    isSelected = reminder.id in selectedIds,
                    onToggleSelect = { onToggleSelect(reminder.id) },
                    onEdit = { onEdit(reminder) },
                    onDelete = { onDelete(reminder) }
                )
            }
        }
    }
}
