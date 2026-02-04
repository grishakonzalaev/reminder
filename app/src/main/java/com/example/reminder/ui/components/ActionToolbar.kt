package com.example.reminder.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActionToolbar(
    context: Context,
    selectionMode: Boolean,
    selectedCount: Int,
    totalCount: Int,
    hasReminders: Boolean,
    onExitSelectionMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selectionMode) {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(Icons.Default.Clear, contentDescription = "Отмена")
                }
                Text(
                    if (selectedCount == 0) "Выбрать" else "Выбрано: $selectedCount",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                TextButton(onClick = onToggleSelectAll) {
                    Text(if (selectedCount == totalCount) "Снять все" else "Выбрать все")
                }
                TextButton(
                    onClick = onDeleteSelected,
                    enabled = selectedCount > 0
                ) {
                    Text(
                        "Удалить",
                        color = if (selectedCount == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                }
            } else {
                if (hasReminders) {
                    TextButton(onClick = onEnterSelectionMode) { Text("Выбрать") }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    TextButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS))
                            } catch (_: Exception) {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                })
                            }
                        }
                    ) { Text("Звонки") }
                }
                TextButton(onClick = onOpenSettings) { Text("Настройки") }
                TextButton(
                    onClick = {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        }
                        context.startActivity(intent)
                    }
                ) { Text("Уведомления") }
            }
        }
    }
}
