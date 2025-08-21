package org.tasks.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.pickers.DatePickerDialog
import org.tasks.compose.pickers.TimePickerDialog
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.hourOfDay
import org.tasks.time.millisOfDay
import org.tasks.time.minuteOfHour
import org.tasks.time.withMillisOfDay
import javax.inject.Inject

@AndroidEntryPoint
class DateAndTimePickerActivity : AppCompatActivity() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var theme: Theme

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TasksTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                var dateSelected by rememberSaveable {
                    mutableLongStateOf(intent.getLongExtra(EXTRA_TIMESTAMP, currentTimeMillis()))
                }
                var showTimePicker by rememberSaveable { mutableStateOf(false) }
                if (showTimePicker) {
                    TimePickerDialog(
                        state = rememberTimePickerState(
                            initialHour = dateSelected.hourOfDay,
                            initialMinute = dateSelected.minuteOfHour,
                            is24Hour = is24HourFormat
                        ),
                        initialDisplayMode = remember { preferences.timeDisplayMode },
                        setDisplayMode = { preferences.timeDisplayMode = it },
                        selected = {
                            val data = Intent()
                            data.putExtras(intent)
                            data.putExtra(
                                EXTRA_TIMESTAMP,
                                DateTime(dateSelected).withMillisOfDay(it).millis
                            )
                            setResult(RESULT_OK, data)
                            finish()
                        },
                        dismiss = { showTimePicker = false },
                    )
                } else {
                    DatePickerDialog(
                        initialDate = remember {
                            intent.getLongExtra(
                                EXTRA_TIMESTAMP,
                                currentTimeMillis()
                            )
                        },
                        displayMode = remember { preferences.calendarDisplayMode },
                        setDisplayMode = {
                            preferences.calendarDisplayMode = it
                        },
                        selected = {
                            dateSelected = it.withMillisOfDay(dateSelected.millisOfDay)
                            showTimePicker = true
                        },
                        dismiss = {
                            finish()
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_TIMESTAMP = "extra_timestamp"
    }
}