package com.example.reminder.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminder.R
import com.example.reminder.data.model.Reminder
import com.example.reminder.ui.components.ActionToolbar
import com.example.reminder.ui.components.AppTopBar
import com.example.reminder.ui.components.EmptyReminderState
import com.example.reminder.ui.components.ReminderList
import com.example.reminder.ui.dialogs.ConfirmationDialog
import com.example.reminder.ui.dialogs.ReminderDialog
import com.example.reminder.ui.viewmodel.ReminderViewModel

@Composable
fun ReminderScreen(viewModel: ReminderViewModel = viewModel()) {
    val reminders by viewModel.reminders.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<Reminder?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var reminderToDelete by remember { mutableStateOf<Reminder?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    val ctx = LocalContext.current

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedIds = emptySet()
    }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            selectionMode = selectionMode,
            onAddClick = { showAddDialog = true }
        )

        ActionToolbar(
            context = ctx,
            selectionMode = selectionMode,
            selectedCount = selectedIds.size,
            totalCount = reminders.size,
            hasReminders = reminders.isNotEmpty(),
            onExitSelectionMode = {
                selectionMode = false
                selectedIds = emptySet()
            },
            onToggleSelectAll = {
                selectedIds = if (selectedIds.size == reminders.size) {
                    emptySet()
                } else {
                    reminders.map { it.id }.toSet()
                }
            },
            onDeleteSelected = {
                if (selectedIds.isNotEmpty()) {
                    showBulkDeleteConfirm = true
                }
            },
            onEnterSelectionMode = { selectionMode = true },
            onOpenSettings = { showSettings = true }
        )

        if (reminders.isEmpty()) {
            EmptyReminderState()
        } else {
            ReminderList(
                reminders = reminders,
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                onToggleSelect = { id ->
                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                },
                onEdit = { reminderToEdit = it },
                onDelete = { reminderToDelete = it },
                modifier = Modifier.weight(1f, fill = true)
            )
        }
    }

    if (showAddDialog) {
        ReminderDialog(
            reminder = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { message, timeMillis ->
                viewModel.addReminder(message, timeMillis)
                showAddDialog = false
            }
        )
    }

    reminderToEdit?.let { reminder ->
        ReminderDialog(
            reminder = reminder,
            onDismiss = { reminderToEdit = null },
            onConfirm = { message, timeMillis ->
                viewModel.updateReminder(reminder, message, timeMillis)
                reminderToEdit = null
            }
        )
    }

    reminderToDelete?.let { reminder ->
        ConfirmationDialog(
            title = stringResource(R.string.dialog_delete_reminder),
            message = stringResource(R.string.dialog_delete_reminder_message, reminder.message),
            onDismiss = { reminderToDelete = null },
            onConfirm = {
                viewModel.deleteReminder(reminder)
                reminderToDelete = null
            }
        )
    }

    if (showBulkDeleteConfirm) {
        val toDelete = reminders.filter { it.id in selectedIds }
        ConfirmationDialog(
            title = stringResource(R.string.dialog_delete_reminders),
            message = stringResource(R.string.dialog_delete_reminders_message, toDelete.size),
            onDismiss = { showBulkDeleteConfirm = false },
            onConfirm = {
                viewModel.deleteReminders(toDelete)
                showBulkDeleteConfirm = false
                selectionMode = false
                selectedIds = emptySet()
            }
        )
    }
}

