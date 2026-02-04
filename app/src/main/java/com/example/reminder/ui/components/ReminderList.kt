package com.example.reminder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.reminder.data.model.Reminder

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
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(reminders) { reminder ->
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
