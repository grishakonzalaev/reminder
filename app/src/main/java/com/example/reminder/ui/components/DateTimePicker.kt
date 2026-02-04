package com.example.reminder.ui.components

import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class AccessibleFrameLayout(context: Context) : FrameLayout(context) {
    var currentValue: Float = 0f
    var currentLabel: String = ""

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return false
    }
}

@Composable
fun AdjustableValue(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
    roleDescription: String,
    modifier: Modifier = Modifier
) {
    val step = 1f

    Box(
        modifier = modifier.height(48.dp)
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = ((valueRange.endInclusive - valueRange.start) / step - 1).roundToInt().coerceAtLeast(0),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .clearAndSetSemantics { }
        )

        AndroidView(
            factory = { context ->
                AccessibleFrameLayout(context).apply {
                    currentValue = value
                    currentLabel = label
                    isFocusable = true
                    isClickable = false
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

                    ViewCompat.setAccessibilityDelegate(this, object : androidx.core.view.AccessibilityDelegateCompat() {
                        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                            super.onInitializeAccessibilityNodeInfo(host, info)
                            val customHost = host as? AccessibleFrameLayout
                            info.className = "android.widget.SeekBar"
                            info.roleDescription = roleDescription
                            info.isScrollable = true
                            info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD)
                            info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD)
                            info.contentDescription = customHost?.currentLabel ?: label
                            info.setText(customHost?.currentLabel ?: label)
                        }

                        override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                            val customHost = host as? AccessibleFrameLayout ?: return super.performAccessibilityAction(host, action, args)
                            when (action) {
                                AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD -> {
                                    val currentVal = customHost.currentValue
                                    val newValue = (currentVal + step).coerceAtMost(valueRange.endInclusive)
                                    if (newValue != currentVal) {
                                        onValueChange(newValue)
                                        return true
                                    }
                                }
                                AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD -> {
                                    val currentVal = customHost.currentValue
                                    val newValue = (currentVal - step).coerceAtLeast(valueRange.start)
                                    if (newValue != currentVal) {
                                        onValueChange(newValue)
                                        return true
                                    }
                                }
                            }
                            return super.performAccessibilityAction(host, action, args)
                        }
                    })
                }
            },
            update = { view ->
                (view as? AccessibleFrameLayout)?.let {
                    it.currentValue = value
                    it.currentLabel = label
                    view.contentDescription = label
                    view.announceForAccessibility(label)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}

enum class DateTimePickerMode {
    SLIDER, TEXT
}

@Composable
fun DateTimePickerSliders(
    timeMillis: Long,
    onTimeChanged: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickerMode by remember { mutableStateOf(DateTimePickerMode.SLIDER) }

    val calendar = remember(timeMillis) { Calendar.getInstance().apply { this.timeInMillis = timeMillis } }
    var day by remember(timeMillis) { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH).toFloat()) }
    var month by remember(timeMillis) { mutableStateOf(calendar.get(Calendar.MONTH).toFloat()) }
    var year by remember(timeMillis) { mutableStateOf(calendar.get(Calendar.YEAR).toFloat()) }
    var hour by remember(timeMillis) { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY).toFloat()) }
    var minute by remember(timeMillis) { mutableStateOf(calendar.get(Calendar.MINUTE).toFloat()) }

    val locale = Locale.getDefault()
    val dateFormatSymbols = remember(locale) { DateFormatSymbols.getInstance(locale) }
    val monthNamesShort = remember(dateFormatSymbols) { dateFormatSymbols.shortMonths.take(12) }
    val monthNamesFull = remember(dateFormatSymbols) { dateFormatSymbols.months.take(12) }
    val dayNames = remember(dateFormatSymbols) { dateFormatSymbols.weekdays.drop(1) }

    val dateFormatter = remember(locale) { SimpleDateFormat("EEEE, d MMMM yyyy", locale) }

    fun updateTime() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year.toInt())
            set(Calendar.MONTH, month.toInt())
            set(Calendar.DAY_OF_MONTH, day.toInt())
            set(Calendar.HOUR_OF_DAY, hour.toInt())
            set(Calendar.MINUTE, minute.toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        onTimeChanged(cal.timeInMillis)
    }

    val currentCal = remember(day, month, year, hour, minute) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year.toInt())
            set(Calendar.MONTH, month.toInt())
            set(Calendar.DAY_OF_MONTH, day.toInt().coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            set(Calendar.HOUR_OF_DAY, hour.toInt())
            set(Calendar.MINUTE, minute.toInt())
        }
    }

    val fullDateString = remember(currentCal) {
        dateFormatter.format(currentCal.time).replaceFirstChar { it.uppercase() }
    }
    val timeString = "${hour.toInt().toString().padStart(2, '0')}:${minute.toInt().toString().padStart(2, '0')}"

    Column(modifier = modifier) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { pickerMode = DateTimePickerMode.SLIDER },
                selected = pickerMode == DateTimePickerMode.SLIDER,
                label = { Text("Slider") }
            )
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { pickerMode = DateTimePickerMode.TEXT },
                selected = pickerMode == DateTimePickerMode.TEXT,
                label = { Text("Text") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$fullDateString, $timeString",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (pickerMode) {
            DateTimePickerMode.SLIDER -> {
                SliderDateTimePicker(
                    day = day,
                    month = month,
                    year = year,
                    hour = hour,
                    minute = minute,
                    monthNamesShort = monthNamesShort,
                    monthNamesFull = monthNamesFull,
                    onDayChange = { day = it; updateTime() },
                    onMonthChange = { month = it; updateTime() },
                    onYearChange = { year = it; updateTime() },
                    onHourChange = { hour = it; updateTime() },
                    onMinuteChange = { minute = it; updateTime() }
                )
            }
            DateTimePickerMode.TEXT -> {
                TextDateTimePicker(
                    day = day.toInt(),
                    month = month.toInt(),
                    year = year.toInt(),
                    hour = hour.toInt(),
                    minute = minute.toInt(),
                    monthNamesFull = monthNamesFull,
                    onDayChange = { day = it.toFloat(); updateTime() },
                    onMonthChange = { month = it.toFloat(); updateTime() },
                    onYearChange = { year = it.toFloat(); updateTime() },
                    onHourChange = { hour = it.toFloat(); updateTime() },
                    onMinuteChange = { minute = it.toFloat(); updateTime() }
                )
            }
        }
    }
}

@Composable
private fun SliderDateTimePicker(
    day: Float,
    month: Float,
    year: Float,
    hour: Float,
    minute: Float,
    monthNamesShort: List<String>,
    monthNamesFull: List<String>,
    onDayChange: (Float) -> Unit,
    onMonthChange: (Float) -> Unit,
    onYearChange: (Float) -> Unit,
    onHourChange: (Float) -> Unit,
    onMinuteChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = day.toInt().toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .width(60.dp)
                    .clearAndSetSemantics { }
            )
            AdjustableValue(
                value = day,
                onValueChange = onDayChange,
                valueRange = 1f..31f,
                label = day.toInt().toString().padStart(2, '0'),
                roleDescription = "day",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = monthNamesShort[month.toInt()],
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .width(60.dp)
                    .clearAndSetSemantics { }
            )
            AdjustableValue(
                value = month,
                onValueChange = onMonthChange,
                valueRange = 0f..11f,
                label = monthNamesFull[month.toInt()],
                roleDescription = "month",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = year.toInt().toString(),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .width(60.dp)
                    .clearAndSetSemantics { }
            )
            AdjustableValue(
                value = year,
                onValueChange = onYearChange,
                valueRange = Calendar.getInstance().get(Calendar.YEAR).toFloat()..(Calendar.getInstance().get(Calendar.YEAR) + 10).toFloat(),
                label = year.toInt().toString(),
                roleDescription = "year",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = hour.toInt().toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .width(60.dp)
                    .clearAndSetSemantics { }
            )
            AdjustableValue(
                value = hour,
                onValueChange = onHourChange,
                valueRange = 0f..23f,
                label = hour.toInt().toString().padStart(2, '0'),
                roleDescription = "hour",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = minute.toInt().toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .width(60.dp)
                    .clearAndSetSemantics { }
            )
            AdjustableValue(
                value = minute,
                onValueChange = onMinuteChange,
                valueRange = 0f..59f,
                label = minute.toInt().toString().padStart(2, '0'),
                roleDescription = "minute",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TextDateTimePicker(
    day: Int,
    month: Int,
    year: Int,
    hour: Int,
    minute: Int,
    monthNamesFull: List<String>,
    onDayChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    var dayText by remember(day) { mutableStateOf(day.toString()) }
    var yearText by remember(year) { mutableStateOf(year.toString()) }
    var hourText by remember(hour) { mutableStateOf(hour.toString().padStart(2, '0')) }
    var minuteText by remember(minute) { mutableStateOf(minute.toString().padStart(2, '0')) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = dayText,
            onValueChange = { new ->
                if (new.isEmpty() || (new.all { it.isDigit() } && new.length <= 2)) {
                    dayText = new
                    new.toIntOrNull()?.coerceIn(1, 31)?.let(onDayChange)
                }
            },
            label = { Text("Day") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedTextField(
                value = monthNamesFull[month],
                onValueChange = {},
                label = { Text("Month") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMonthDropdown = true }
            )
            DropdownMenu(
                expanded = showMonthDropdown,
                onDismissRequest = { showMonthDropdown = false }
            ) {
                monthNamesFull.forEachIndexed { index, monthName ->
                    DropdownMenuItem(
                        text = { Text(monthName) },
                        onClick = {
                            onMonthChange(index)
                            showMonthDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = yearText,
            onValueChange = { new ->
                if (new.isEmpty() || (new.all { it.isDigit() } && new.length <= 4)) {
                    yearText = new
                    new.toIntOrNull()?.coerceIn(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.YEAR) + 10)?.let(onYearChange)
                }
            },
            label = { Text("Year") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = hourText,
                onValueChange = { new ->
                    if (new.isEmpty() || (new.all { it.isDigit() } && new.length <= 2)) {
                        hourText = new
                        new.toIntOrNull()?.coerceIn(0, 23)?.let(onHourChange)
                    }
                },
                label = { Text("Hour") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = minuteText,
                onValueChange = { new ->
                    if (new.isEmpty() || (new.all { it.isDigit() } && new.length <= 2)) {
                        minuteText = new
                        new.toIntOrNull()?.coerceIn(0, 59)?.let(onMinuteChange)
                    }
                },
                label = { Text("Minute") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
