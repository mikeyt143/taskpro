package org.tasks.sync.microsoft

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.http.HttpClientFactory
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MicrosoftListSettingsActivityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val httpClientFactory: HttpClientFactory,
    private val caldavDao: CaldavDao,
    private val taskDeleter: TaskDeleter,
) : ViewModel() {
    data class ViewState(
        val requestInFlight: Boolean = false,
        val result: CaldavCalendar? = null,
        val error: Throwable? = null,
        val deleted: Boolean = false,
    )

    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asStateFlow()

    val account: CaldavAccount =
        savedStateHandle[BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_ACCOUNT]!!

    val list: CaldavCalendar? =
        savedStateHandle[BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_CALENDAR]

    private suspend fun service(): MicrosoftService =
        httpClientFactory.getMicrosoftService(account)

    suspend fun createList(displayName: String) {
        _viewState.update { it.copy(requestInFlight = true) }
        val taskList = TaskLists.TaskList(displayName = displayName)
        try {
            val result = service().createList(taskList)
            val list = CaldavCalendar(
                account = this@MicrosoftListSettingsActivityViewModel.account.uuid
            ).apply {
                result.applyTo(this)
            }
            caldavDao.insert(list)
            _viewState.update { it.copy(result = list) }
        } catch (e: Exception) {
            requestFailed(e)
        }
    }

    suspend fun deleteList() {
        _viewState.update { it.copy(requestInFlight = true) }
        try {
            if (isDefault()) {
                throw IllegalArgumentException("The default list cannot be deleted")
            }
            val result = service().deleteList(list?.uuid!!)
            taskDeleter.delete(list)
            _viewState.update { it.copy(deleted = true) }
        } catch (e: Exception) {
            requestFailed(e)
        }
    }

    suspend fun updateList(displayName: String) {
        _viewState.update { it.copy(requestInFlight = true) }
        val taskList = TaskLists.TaskList(displayName = displayName)
        try {
            if (isDefault()) {
                throw IllegalArgumentException("The default list cannot be renamed")
            }
            val result = service().updateList(list?.uuid!!, taskList)
            result.applyTo(list)
            caldavDao.update(list)
            _viewState.update { it.copy(result = list) }
        } catch (e: Exception) {
            requestFailed(e)
        }
    }

    fun clearError() {
        _viewState.update { it.copy(error = null) }
    }

    private fun requestFailed(exception: Exception) {
        Timber.e(exception)
        _viewState.update {
            it.copy(
                requestInFlight = false,
                error = exception,
            )
        }
    }

    private suspend fun isDefault(): Boolean = try {
        service().getList(list?.uuid!!).wellknownListName == "defaultList"
    } catch (e: Exception) {
        Timber.e(e)
        false
    }
}
