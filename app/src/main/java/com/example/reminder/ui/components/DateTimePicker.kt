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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.reminder.R
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
    var currentValueRange: ClosedFloatingPointRange<Float> = 0f..1f
    var currentWrap: Boolean = false
    var currentOnValueChange: ((Float) -> Unit)? = null

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
    wrap: Boolean = false,
    modifier: Modifier = Modifier
) {
    val step = 1f

    Box(
        modifier = modifier.height(48.dp)
    ) {
        Slider(
            value = value,
            onValueChange = { newVal ->
                onValueChange(newVal.coerceIn(valueRange))
            },
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
                    currentValueRange = valueRange
                    currentWrap = wrap
                    currentOnValueChange = onValueChange
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
                            val range = customHost.currentValueRange
                            val shouldWrap = customHost.currentWrap
                            val valueChangeFn = customHost.currentOnValueChange ?: return super.performAccessibilityAction(host, action, args)
                            when (action) {
                                AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD -> {
                                    val currentVal = customHost.currentValue
                                    val newValue = if (shouldWrap && currentVal >= range.endInclusive) {
                                        range.start
                                    } else {
                                        (currentVal + step).coerceAtMost(range.endInclusive)
                                    }
                                    valueChangeFn(newValue)
                                    return true
                                }
                                AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD -> {
                                    val currentVal = customHost.currentValue
                                    val newValue = if (shouldWrap && currentVal <= range.start) {
                                        range.endInclusive
                                    } else {
                                        (currentVal - step).coerceAtLeast(range.start)
                                    }
                                    valueChangeFn(newValue)
                                    return true
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
                    it.currentValueRange = valueRange
                    it.currentWrap = wrap
                    it.currentOnValueChange = onValueChange
                    view.contentDescription = label
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

    val monthNamesShort = remember(locale) {
        (0..11).map { monthIndex ->
            SimpleDateFormat("MMM", locale).apply {
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, monthIndex)
                this.calendar = cal
            }.format(Calendar.getInstance().apply { set(Calendar.MONTH, monthIndex) }.time)
        }
    }
    val monthNamesFull = remember(locale) {
        (0..11).map { monthIndex ->
            SimpleDateFormat("LLLL", locale).apply {
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, monthIndex)
                this.calendar = cal
            }.format(Calendar.getInstance().apply { set(Calendar.MONTH, monthIndex) }.time)
        }.map { it.replaceFirstChar { char -> char.uppercase() } }
    }

    val dateFormatter = remember(locale) { SimpleDateFormat("EEEE, d MMMM yyyy", locale) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val yearRange = currentYear.toFloat()..(currentYear + 10).toFloat()

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

    val maxDay = Calendar.getInstance().apply {
        clear()
        set(year.toInt(), month.toInt(), 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH).toFloat()

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
                label = { Text(stringResource(R.string.picker_mode_slider)) }
            )
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { pickerMode = DateTimePickerMode.TEXT },
                selected = pickerMode == DateTimePickerMode.TEXT,
                label = { Text(stringResource(R.string.picker_mode_text)) }
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
                    maxDay = maxDay,
                    yearRange = yearRange,
                    monthNamesShort = monthNamesShort,
                    monthNamesFull = monthNamesFull,
                    onDayChange = { newDay ->
                        day = newDay.coerceAtMost(maxDay)
                        updateTime()
                    },
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
                    yearRange = yearRange,
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
    maxDay: Float,
    yearRange: ClosedFloatingPointRange<Float>,
    monthNamesShort: List<String>,
    monthNamesFull: List<String>,
    onDayChange: (Float) -> Unit,
    onMonthChange: (Float) -> Unit,
    onYearChange: (Float) -> Unit,
    onHourChange: (Float) -> Unit,
    onMinuteChange: (Float) -> Unit
) {
    val dayRole = stringResource(R.string.picker_day)
    val monthRole = stringResource(R.string.picker_month)
    val yearRole = stringResource(R.string.picker_year)
    val hourRole = stringResource(R.string.picker_hour)
    val minuteRole = stringResource(R.string.picker_minute)

    Column {
        DateTimeSliderRow(
            value = day.coerceAtMost(maxDay),
            displayText = day.toInt().coerceAtMost(maxDay.toInt()).toString(),
            valueRange = 1f..maxDay,
            roleDescription = dayRole,
            label = day.toInt().coerceAtMost(maxDay.toInt()).toString(),
            wrap = true,
            onValueChange = onDayChange
        )
        Spacer(modifier = Modifier.height(8.dp))

        DateTimeSliderRow(
            value = month,
            displayText = monthNamesShort[month.toInt()],
            valueRange = 0f..11f,
            roleDescription = monthRole,
            label = monthNamesFull[month.toInt()],
            wrap = true,
            onValueChange = onMonthChange
        )
        Spacer(modifier = Modifier.height(8.dp))

        DateTimeSliderRow(
            value = year,
            displayText = year.toInt().toString(),
            valueRange = yearRange,
            roleDescription = yearRole,
            label = year.toInt().toString(),
            wrap = false,
            onValueChange = onYearChange
        )
        Spacer(modifier = Modifier.height(16.dp))

        DateTimeSliderRow(
            value = hour,
            displayText = hour.toInt().toString().padStart(2, '0'),
            valueRange = 0f..23f,
            roleDescription = hourRole,
            label = hour.toInt().toString().padStart(2, '0'),
            wrap = true,
            onValueChange = onHourChange
        )
        Spacer(modifier = Modifier.height(8.dp))

        DateTimeSliderRow(
            value = minute,
            displayText = minute.toInt().toString().padStart(2, '0'),
            valueRange = 0f..59f,
            roleDescription = minuteRole,
            label = minute.toInt().toString().padStart(2, '0'),
            wrap = true,
            onValueChange = onMinuteChange
        )
    }
}

@Composable
private fun DateTimeSliderRow(
    value: Float,
    displayText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    roleDescription: String,
    label: String,
    wrap: Boolean = false,
    onValueChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .width(60.dp)
                .clearAndSetSemantics { }
        )
        AdjustableValue(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            label = label,
            roleDescription = roleDescription,
            wrap = wrap,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TextDateTimePicker(
    day: Int,
    month: Int,
    year: Int,
    hour: Int,
    minute: Int,
    yearRange: ClosedFloatingPointRange<Float>,
    monthNamesFull: List<String>,
    onDayChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    var showMonthDropdown by remember { mutableStateOf(false) }

    Column {
        NumberTextField(
            value = day,
            label = stringResource(R.string.picker_day),
            range = 1..31,
            maxLength = 2,
            onValueChange = onDayChange,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedTextField(
                value = monthNamesFull[month],
                onValueChange = {},
                label = { Text(stringResource(R.string.picker_month)) },
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

        NumberTextField(
            value = year,
            label = stringResource(R.string.picker_year),
            range = yearRange.start.toInt()..yearRange.endInclusive.toInt(),
            maxLength = 4,
            onValueChange = onYearChange,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberTextField(
                value = hour,
                label = stringResource(R.string.picker_hour),
                range = 0..23,
                maxLength = 2,
                padStart = 2,
                onValueChange = onHourChange,
                modifier = Modifier.weight(1f)
            )
            NumberTextField(
                value = minute,
                label = stringResource(R.string.picker_minute),
                range = 0..59,
                maxLength = 2,
                padStart = 2,
                onValueChange = onMinuteChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NumberTextField(
    value: Int,
    label: String,
    range: IntRange,
    maxLength: Int,
    padStart: Int = 0,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) {
        mutableStateOf(if (padStart > 0) value.toString().padStart(padStart, '0') else value.toString())
    }

    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            if (new.isEmpty() || (new.all { it.isDigit() } && new.length <= maxLength)) {
                text = new
                new.toIntOrNull()?.coerceIn(range)?.let(onValueChange)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}
