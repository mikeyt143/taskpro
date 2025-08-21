package org.tasks.preferences

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.auth.SignInActivity
import org.tasks.extensions.Context.toast
import org.tasks.jobs.WorkManager
import org.tasks.preferences.fragments.MainSettingsFragment
import org.tasks.preferences.fragments.MainSettingsFragment.Companion.REQUEST_CALDAV_SETTINGS
import org.tasks.preferences.fragments.MainSettingsFragment.Companion.REQUEST_GOOGLE_TASKS
import org.tasks.preferences.fragments.MainSettingsFragment.Companion.REQUEST_TASKS_ORG
import org.tasks.preferences.fragments.TasksAccount
import org.tasks.sync.SyncAdapters
import javax.inject.Inject

@AndroidEntryPoint
class MainPreferences : BasePreferences() {

    @Inject lateinit var syncAdapters: SyncAdapters
    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private val viewModel: PreferencesViewModel by viewModels()

    override fun getRootTitle() = R.string.TLA_menu_settings

    override fun getRootPreference() = MainSettingsFragment()

    override fun onResume() {
        super.onResume()

        localBroadcastManager.registerPreferenceReceiver(refreshReceiver)

        viewModel.updateBackups()
    }

    override fun onPause() {
        super.onPause()

        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CALDAV_SETTINGS -> if (resultCode == RESULT_OK) {
                syncAdapters.sync(true)
                workManager.updateBackgroundSync()
            }
            REQUEST_GOOGLE_TASKS -> if (resultCode == Activity.RESULT_OK) {
                syncAdapters.sync(true)
                workManager.updateBackgroundSync()
            } else {
                data?.getStringExtra(GtasksLoginActivity.EXTRA_ERROR)?.let { toast(it) }
            }
            REQUEST_TASKS_ORG -> if (resultCode == Activity.RESULT_OK) {
                syncAdapters.sync(true)
                workManager.updateBackgroundSync()
                lifecycleScope.launch {
                    val account = viewModel.tasksAccount() ?: return@launch
                    val fragment = supportFragmentManager.findFragmentById(R.id.settings)
                    if (fragment is PreferenceFragmentCompat && fragment !is TasksAccount) {
                        startPreference(
                                fragment,
                                TasksAccount.newTasksAccountPreference(account),
                                getString(R.string.tasks_org)
                        )
                    }
                }
            } else {
                data?.getStringExtra(SignInActivity.EXTRA_ERROR)?.let { toast(it) }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.updateBackups()
        }
    }
}