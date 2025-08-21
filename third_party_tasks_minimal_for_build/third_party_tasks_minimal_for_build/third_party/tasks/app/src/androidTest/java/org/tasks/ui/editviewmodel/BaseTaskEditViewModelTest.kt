package org.tasks.ui.editviewmodel

import androidx.lifecycle.SavedStateHandle
import com.todoroo.astrid.activity.TaskEditFragment
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskDeleter
import com.todoroo.astrid.service.TaskMover
import com.todoroo.astrid.timers.TimerPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.tasks.calendars.CalendarEventProvider
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.db.Database
import org.tasks.data.entity.Task
import org.tasks.data.newLocalAccount
import org.tasks.injection.InjectingTestCase
import org.tasks.location.GeofenceApi
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.PermissivePermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.ui.TaskEditViewModel
import javax.inject.Inject

open class BaseTaskEditViewModelTest : InjectingTestCase() {
    @Inject lateinit var db: Database
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var timerPlugin: TimerPlugin
    @Inject lateinit var calendarEventProvider: CalendarEventProvider
    @Inject lateinit var gCalHelper: GCalHelper
    @Inject lateinit var taskMover: TaskMover
    @Inject lateinit var geofenceApi: GeofenceApi
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject lateinit var alarmService: AlarmService
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var alarmDao: AlarmDao
    @Inject lateinit var userActivityDao: UserActivityDao
    @Inject lateinit var caldavDao: CaldavDao

    protected lateinit var viewModel: TaskEditViewModel

    @Before
    override fun setUp() {
        runBlocking {
            super.setUp()
            caldavDao.newLocalAccount()
        }
    }

    protected fun setup(task: Task) = runBlocking {
        viewModel = TaskEditViewModel(
            context,
            SavedStateHandle().apply {
                set(TaskEditFragment.EXTRA_TASK, task)
            },
            taskDao,
            taskDeleter,
            timerPlugin,
            PermissivePermissionChecker(context),
            calendarEventProvider,
            gCalHelper,
            taskMover,
            db.locationDao(),
            geofenceApi,
            db.tagDao(),
            db.tagDataDao(),
            preferences,
            db.googleTaskDao(),
            db.caldavDao(),
            taskCompleter,
            alarmService,
            MutableSharedFlow(),
            userActivityDao = userActivityDao,
            taskAttachmentDao = db.taskAttachmentDao(),
            alarmDao = db.alarmDao(),
            defaultFilterProvider = defaultFilterProvider,
        )
    }

    protected fun save(): Boolean = runBlocking(Dispatchers.Main) {
        viewModel.save()
    }
}