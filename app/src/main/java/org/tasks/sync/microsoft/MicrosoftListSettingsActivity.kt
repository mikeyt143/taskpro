package org.tasks.sync.microsoft

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.filters.CaldavFilter
import org.tasks.themes.TasksTheme

@AndroidEntryPoint
class MicrosoftListSettingsActivity : BaseCaldavCalendarSettingsActivity() {
    private val viewModel: MicrosoftListSettingsActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launchWhenResumed {
            viewModel
                .viewState
                .collect { state ->
                    state.error?.let { throwable ->
                        requestFailed(throwable)
                        viewModel.clearError()
                    }
                    if (state.deleted) {
                        setResult(Activity.RESULT_OK, Intent(TaskListFragment.ACTION_DELETED))
                        finish()
                    }
                    state.result?.let {
                        setResult(
                            Activity.RESULT_OK,
                            Intent(TaskListFragment.ACTION_RELOAD).putExtra(
                                MainActivity.OPEN_FILTER,
                                CaldavFilter(it, viewModel.account)
                            )
                        )
                        finish()
                    }
                }
        }

        setContent {
            TasksTheme {
                BaseCaldavSettingsContent()
            }
        }
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) =
        viewModel.createList(name)

    override suspend fun updateNameAndColor(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        name: String,
        color: Int
    ) = viewModel.updateList(name)

    override suspend fun deleteCalendar(
        caldavAccount: CaldavAccount,
        caldavCalendar: CaldavCalendar
    ) = viewModel.deleteList()
}