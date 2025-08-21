package org.tasks.preferences.fragments

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForFilterPickerResult
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.DefaultFilterProvider
import javax.inject.Inject

@AndroidEntryPoint
class DashClock : InjectingPreferenceFragment() {

    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private val listPickerLauncher = registerForFilterPickerResult {
        defaultFilterProvider.dashclockFilter = it
        lifecycleScope.launch {
            refreshPreferences()
        }
        localBroadcastManager.broadcastRefresh()
    }

    override fun getPreferenceXml() = R.xml.preferences_dashclock

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        findPreference(R.string.p_dashclock_filter)
            .setOnPreferenceClickListener {
                listPickerLauncher.launch(
                    context = requireContext(),
                    selectedFilter = defaultFilterProvider.dashclockFilter,
                )
                false
            }

        refreshPreferences()
    }

    private suspend fun refreshPreferences() {
        val filter = defaultFilterProvider.getFilterFromPreference(R.string.p_dashclock_filter)
        findPreference(R.string.p_dashclock_filter).summary = filter.title
    }
}