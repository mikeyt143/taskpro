package com.todoroo.astrid.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.astrid.activity.MainActivity.Companion.LOAD_FILTER
import com.todoroo.astrid.activity.MainActivity.Companion.OPEN_FILTER
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.LocalBroadcastManager
import org.tasks.TasksApplication.Companion.IS_GENERIC
import org.tasks.billing.Inventory
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.throttleLatest
import org.tasks.data.NO_COUNT
import org.tasks.data.count
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterProvider
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.SearchFilter
import org.tasks.filters.getIcon
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.TasksPreferences
import org.tasks.themes.ColorProvider
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val filterProvider: FilterProvider,
    private val taskDao: TaskDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val inventory: Inventory,
    private val colorProvider: ColorProvider,
    private val caldavDao: CaldavDao,
    private val tasksPreferences: TasksPreferences,
) : ViewModel() {

    data class State(
        val begForMoney: Boolean = false,
        val filter: Filter,
        val task: Task? = null,
        val drawerItems: ImmutableList<DrawerItem> = persistentListOf(),
        val searchItems: ImmutableList<DrawerItem> = persistentListOf(),
        val menuQuery: String = "",
    )

    private val _drawerOpen = MutableStateFlow(false)
    private val _updateFilters = MutableStateFlow(0L)

    private val _state = MutableStateFlow(
        State(
            filter = savedStateHandle.get<Filter>(OPEN_FILTER)
                ?: savedStateHandle.get<String>(LOAD_FILTER)?.let {
                    runBlocking { defaultFilterProvider.getFilterFromPreference(it) }
                }
                ?: runBlocking { defaultFilterProvider.getStartupFilter() },
            begForMoney = if (IS_GENERIC) !inventory.hasTasksAccount else !inventory.hasPro,
        )
    )
    val state = _state.asStateFlow()

    val accountExists: Flow<Boolean>
        get() = caldavDao.watchAccountExists()

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                LocalBroadcastManager.REFRESH,
                LocalBroadcastManager.REFRESH_LIST -> _updateFilters.update { currentTimeMillis() }
            }
        }
    }

    suspend fun resetFilter() {
        setFilter(defaultFilterProvider.getDefaultOpenFilter())
    }

    fun setFilter(
        filter: Filter,
        task: Task? = null,
    ) {
        if (filter == _state.value.filter && task == null) {
            return
        }
        _state.update {
            it.copy(
                filter = filter,
                task = task,
            )
        }
        updateFilters()
        if (filter !is SearchFilter) {
            defaultFilterProvider.setLastViewedFilter(filter)
        }
    }

    fun setDrawerState(opened: Boolean) {
        _drawerOpen.update { opened }
        if (!opened) {
            _state.update { it.copy(menuQuery = "") }
        }
    }

    init {
        localBroadcastManager.registerRefreshListReceiver(refreshReceiver)

        _updateFilters
            .onStart { updateFilters() }
            .combine(_drawerOpen) { timestamp, drawerOpen ->
                if (drawerOpen) timestamp else null
            }
            .filterNotNull()
            .throttleLatest(1000)
            .onEach { updateFilters() }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    private fun updateFilters() = viewModelScope.launch(Dispatchers.IO) {
        val selected = state.value.filter
        filterProvider
            .drawerItems()
            .map { item ->
                when (item) {
                    is Filter ->
                        DrawerItem.Filter(
                            title = item.title ?: "",
                            icon = item.getIcon(inventory),
                            color = getColor(item),
                            count = item.count.takeIf { it != NO_COUNT } ?: try {
                                taskDao.count(item)
                            } catch (e: Exception) {
                                Timber.e(e)
                                0
                            },
                            selected = item.areItemsTheSame(selected),
                            shareCount = if (item is CaldavFilter) item.principals else 0,
                            filter = item,
                        )
                    is NavigationDrawerSubheader ->
                        DrawerItem.Header(
                            title = item.title ?: "",
                            collapsed = item.isCollapsed,
                            hasError = item.error,
                            canAdd = item.addIntentRc != 0,
                            header = item,
                        )
                    else -> throw IllegalArgumentException()
                }
            }
            .let { filters -> _state.update { it.copy(drawerItems = filters.toPersistentList()) } }
        val query = _state.value.menuQuery
        filterProvider
            .allFilters()
            .filter { it.title!!.contains(query, ignoreCase = true) }
            .map { item ->
                DrawerItem.Filter(
                    title = item.title ?: "",
                    icon = item.getIcon(inventory),
                    color = getColor(item),
                    count = item.count.takeIf { it != NO_COUNT } ?: try {
                        taskDao.count(item)
                    } catch (e: Exception) {
                        Timber.e(e)
                        0
                    },
                    selected = item.areItemsTheSame(selected),
                    shareCount = if (item is CaldavFilter) item.principals else 0,
                    filter = item,
                )
            }
            .let { filters -> _state.update { it.copy(searchItems = filters.toPersistentList()) } }
    }

    private fun getColor(filter: Filter): Int {
        if (filter.tint != 0) {
            val color = colorProvider.getThemeColor(filter.tint, true)
            if (color.isFree || inventory.purchasedThemes()) {
                return color.primaryColor
            }
        }
        return 0
    }

    fun toggleCollapsed(subheader: NavigationDrawerSubheader) = viewModelScope.launch {
        val collapsed = !subheader.isCollapsed
        when (subheader.subheaderType) {
            NavigationDrawerSubheader.SubheaderType.PREFERENCE -> {
                tasksPreferences.set(booleanPreferencesKey(subheader.id), collapsed)
                localBroadcastManager.broadcastRefreshList()
            }
            NavigationDrawerSubheader.SubheaderType.CALDAV,
            NavigationDrawerSubheader.SubheaderType.TASKS -> {
                caldavDao.setCollapsed(subheader.id, collapsed)
                localBroadcastManager.broadcastRefreshList()
            }
        }
    }

    fun setTask(task: Task?) {
        _state.update { it.copy(task = task) }
    }

    fun queryMenu(query: String) {
        _state.update { it.copy(menuQuery = query) }
        updateFilters()
    }

    suspend fun getAccount(id: Long) = caldavDao.getAccount(id)

    fun openLastViewedFilter() = viewModelScope.launch {
        setFilter(defaultFilterProvider.getLastViewedFilter())
    }
}
