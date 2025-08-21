package org.tasks.compose.pickers

import android.content.res.Configuration
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.R
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTime
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: Long,
    displayMode: DisplayMode,
    setDisplayMode: (DisplayMode) -> Unit,
    selected: (Long) -> Unit,
    dismiss: () -> Unit,
) {
    val initialDateUTC by remember(initialDate) {
        derivedStateOf {
            // DateTime(initialDate).toUTC().millis
            // DateTime.toUTC() does not change DateTime.millis value, but DatePicker expects it
            // is in local timezone and decrements it by TimeZone.offset. This shifts the date to
            // the previous date in timezones to East of GMT, which is unexpected
            DateTime(initialDate).let { it.millis + it.offset }
        }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateUTC,
        initialDisplayMode = displayMode,
    )
    LaunchedEffect(datePickerState.displayMode) {
        Timber.d("Set display mode to ${datePickerState.displayMode}")
        setDisplayMode(datePickerState.displayMode)
    }
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = { dismiss() },
        dismissButton = {
                TextButton(onClick = dismiss) {
                    Text(text = stringResource(id = R.string.cancel))
                }
        },
        confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState
                            .selectedDateMillis
                            ?.let { selected(it - DateTime(it).offset) }
                    }
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DatePickerPreview() {
    TasksTheme {
        DatePickerDialog(
            initialDate = DateTime().plusDays(1).millis,
            displayMode = DisplayMode.Picker,
            setDisplayMode = {},
            selected = {},
            dismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DatePickerPreviewInput() {
    TasksTheme {
        DatePickerDialog(
            initialDate = DateTime().plusDays(1).millis,
            displayMode = DisplayMode.Input,
            setDisplayMode = {},
            selected = {},
            dismiss = {}
        )
    }
}
