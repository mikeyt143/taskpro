package org.tasks.calendars

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.pickers.CalendarPicker
import org.tasks.dialogs.DialogBuilder
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class CalendarPicker : DialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var theme: Theme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return dialogBuilder
            .newDialog()
            .setContent {
                TasksTheme(
                    theme = theme.themeBase.index,
                    primary = theme.themeColor.primaryColor,
                ) {
                    CalendarPicker(
                        selected = arguments?.getString(EXTRA_SELECTED),
                        onSelected = {
                            targetFragment!!.onActivityResult(
                                targetRequestCode,
                                Activity.RESULT_OK,
                                Intent().putExtra(EXTRA_CALENDAR_ID, it?.id)
                            )
                            dismiss()
                        },
                    )
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_CALENDAR_ID = "extra_calendar_id"
        private const val EXTRA_SELECTED = "extra_selected"

        fun newCalendarPicker(target: Fragment?, rc: Int, selected: String?) =
            CalendarPicker().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_SELECTED, selected)
                }
                setTargetFragment(target, rc)
            }
    }
}