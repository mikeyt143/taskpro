package org.tasks.gtasks

import android.content.Context
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.util.DateTime
import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.TaskList
import com.google.api.services.tasks.model.Tasks
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.gtasks.GtasksListService
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities
import com.todoroo.astrid.gtasks.api.GtasksInvoker
import com.todoroo.astrid.gtasks.api.HttpNotFoundException
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.service.TaskCreator.Companion.getDefaultAlarms
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.data.createDueDate
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.googleapis.InvokerFactory
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.io.EOFException
import java.io.IOException
import java.net.HttpRetryException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Collections
import javax.inject.Inject
import javax.net.ssl.SSLException
import kotlin.math.max

class GoogleTaskSynchronizer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val caldavDao: CaldavDao,
    private val gtasksListService: GtasksListService,
    private val preferences: Preferences,
    private val taskDao: TaskDao,
    private val firebase: Firebase,
    private val googleTaskDao: GoogleTaskDao,
    private val taskCreator: TaskCreator,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val permissionChecker: PermissionChecker,
    private val googleAccountManager: GoogleAccountManager,
    private val localBroadcastManager: LocalBroadcastManager,
    private val taskDeleter: TaskDeleter,
    private val invokers: InvokerFactory,
    private val alarmDao: AlarmDao,
) {
    suspend fun sync(account: CaldavAccount) {
        Timber.d("%s: start sync", account)
        try {
            synchronize(account)
        } catch (e: SocketTimeoutException) {
            Timber.e(e)
            account.error = e.message
        } catch (e: SSLException) {
            Timber.e(e)
            account.error = e.message
        } catch (e: SocketException) {
            Timber.e(e)
            account.error = e.message
        } catch (e: UnknownHostException) {
            Timber.e(e)
            account.error = e.message
        } catch (e: HttpRetryException) {
            Timber.e(e)
            account.error = e.message
        } catch (e: EOFException) {
            Timber.e(e)
            account.error = e.message
        } catch (e: GoogleJsonResponseException) {
            account.error = e.message
            when (e.statusCode) {
                401, 503 -> Timber.e(e)
                else -> firebase.reportException(e)
            }
        } catch (e: Exception) {
            account.error = e.message
            firebase.reportException(e)
        } finally {
            caldavDao.update(account)
            localBroadcastManager.broadcastRefreshList()
            Timber.d("%s: end sync", account)
        }
    }

    @Throws(IOException::class)
    private suspend fun synchronize(account: CaldavAccount) {
        if (googleAccountManager.getAccount(account.username) == null) {
            account.error = context.getString(R.string.cannot_access_account)
            return
        }
        val gtasksInvoker = invokers.getGtasksInvoker(account.username!!)
        val gtaskLists: MutableList<TaskList> = ArrayList()
        var nextPageToken: String? = null
        var eTag: String? = null
        do {
            val remoteLists = gtasksInvoker.allGtaskLists(nextPageToken) ?: break
            eTag = remoteLists.etag
            val items = remoteLists.items
            if (items != null) {
                gtaskLists.addAll(items)
            }
            nextPageToken = remoteLists.nextPageToken
        } while (!isNullOrEmpty(nextPageToken))
        gtasksListService.updateLists(account, gtaskLists)
        val defaultRemoteList = defaultFilterProvider.defaultList
        if (defaultRemoteList.isGoogleTasks) {
            val list = caldavDao.getCalendarByUuid(defaultRemoteList.uuid)
            if (list == null) {
                preferences.setString(R.string.p_default_list, null)
            }
        }
        val failedTasks = mutableSetOf<Long>()
        var retryTaskId = pushLocalChanges(account, gtasksInvoker)
        
        while (retryTaskId != null) {
            if (failedTasks.contains(retryTaskId)) {
                throw IOException("Invalid Task ID: $retryTaskId")
            }
            failedTasks.add(retryTaskId)
            
            Timber.d("Retrying push local changes due to stale task ID $retryTaskId (${failedTasks.size} total failed tasks)")
            
            delay(1000)
            
            retryTaskId = pushLocalChanges(account, gtasksInvoker)
        }
        for (list in caldavDao.getCalendarsByAccount(account.uuid!!)) {
            if (isNullOrEmpty(list.uuid)) {
                firebase.reportException(RuntimeException("Empty remote id"))
                continue
            }
            fetchAndApplyRemoteChanges(gtasksInvoker, list)
            gtasksInvoker.updatePositions(list.uuid!!)
        }
//        account.etag = eTag
        account.error = ""
    }

    @Throws(IOException::class)
    private suspend fun GtasksInvoker.updatePositions(list: String) {
        // Unfortunately this is necessary because Google broke the API
        // https://issuetracker.google.com/issues/132432317
        Timber.d("updatePositions(list=${list})")
        fetchPositions(list).forEach { task ->
            googleTaskDao.updatePosition(task.id, task.parent, task.position)
        }
        googleTaskDao.reposition(caldavDao, list)
    }

    @Throws(IOException::class)
    private suspend fun GtasksInvoker.fetchPositions(listId: String): List<Task> {
        val tasks: MutableList<Task> = ArrayList()
        var nextPageToken: String? = null
        do {
            val taskList = getAllPositions(listId, nextPageToken)
            taskList?.items?.let {
                tasks.addAll(it)
            }
            nextPageToken = taskList?.nextPageToken
        } while (!isNullOrEmpty(nextPageToken))
        return tasks
    }

    @Throws(IOException::class)
    private suspend fun pushLocalChanges(account: CaldavAccount, gtasksInvoker: GtasksInvoker): Long? {
        val tasks = taskDao.getGoogleTasksToPush(account.uuid!!)
        for (task in tasks) {
            val staleTaskId = pushTask(task, gtasksInvoker)
            if (staleTaskId != null) {
                return staleTaskId
            }
        }
        return null
    }

    @Throws(IOException::class)
    private suspend fun pushTask(task: org.tasks.data.entity.Task, gtasksInvoker: GtasksInvoker): Long? {
        for (deleted in googleTaskDao.getDeletedByTaskId(task.id)) {
            deleted.remoteId?.let {
                try {
                    gtasksInvoker.deleteGtask(deleted.calendar, it)
                } catch (e: GoogleJsonResponseException) {
                    when (e.statusCode) {
                        400 -> Timber.e(e)
                        else -> throw e
                    }
                }
            }
            googleTaskDao.delete(deleted)
        }
        val gtasksMetadata = googleTaskDao.getByTaskId(task.id) ?: return null
        val remoteModel = Task()
        var newlyCreated = false
        val remoteId: String?
        val defaultRemoteList = defaultFilterProvider.defaultList
        var listId = if (defaultRemoteList.isGoogleTasks) defaultRemoteList.uuid else DEFAULT_LIST
        if (isNullOrEmpty(gtasksMetadata.remoteId)) { // Create case
            gtasksMetadata.calendar?.takeIf { it.isNotBlank() }?.let {
                listId = it
            }
            newlyCreated = true
        } else { // update case
            remoteId = gtasksMetadata.remoteId
            listId = gtasksMetadata.calendar!!
            remoteModel.id = remoteId
        }

        // If task was newly created but without a title, don't sync--we're in the middle of
        // creating a task which may end up being cancelled. Also don't sync new but already
        // deleted tasks
        if (newlyCreated && (isNullOrEmpty(task.title) || task.deletionDate > 0)) {
            return null
        }

        // Update the remote model's changed properties
        if (task.isDeleted) {
            remoteModel.deleted = true
        }
        remoteModel.title = truncate(task.title, MAX_TITLE_LENGTH)
        remoteModel.notes = truncate(task.notes, MAX_DESCRIPTION_LENGTH)
        if (task.hasDueDate()) {
            remoteModel.due = GtasksApiUtilities.unixTimeToGtasksDueDate(task.dueDate).toStringRfc3339()
        }
        if (task.isCompleted) {
            remoteModel.completed = GtasksApiUtilities.unixTimeToGtasksCompletionTime(task.completionDate).toStringRfc3339()
            remoteModel.status = "completed" // $NON-NLS-1$
        } else {
            remoteModel.completed = null
            remoteModel.status = "needsAction" // $NON-NLS-1$
        }
        if (newlyCreated) {
            val parent = task.parent
            val localParent = if (parent > 0) googleTaskDao.getRemoteId(parent) else null
            val previous = googleTaskDao.getPrevious(
                    listId, if (isNullOrEmpty(localParent)) 0 else parent, task.order ?: 0)
            val created: Task? = try {
                gtasksInvoker.createGtask(listId, remoteModel, localParent, previous)
            } catch (e: HttpNotFoundException) {
                Timber.e(e, "Failed to create task, retry without parent or order")
                gtasksInvoker.createGtask(listId, remoteModel, null, null)
            }
            if (created != null) {
                // Update the metadata for the newly created task
                gtasksMetadata.remoteId = created.id
                gtasksMetadata.calendar = listId
                setOrderAndParent(gtasksMetadata, created, task)
                Timber.d("Created new task: $gtasksMetadata")
            } else {
                Timber.e("Empty response when creating task")
                return null
            }
        } else {
            try {
                if (!task.isDeleted && gtasksMetadata.isMoved) {
                    try {
                        val parent = task.parent
                        val localParent = if (parent > 0) googleTaskDao.getRemoteId(parent) else null
                        val previous = googleTaskDao.getPrevious(
                                listId,
                                if (localParent.isNullOrBlank()) 0 else parent,
                                task.order ?: 0,
                        )
                        gtasksInvoker
                                .moveGtask(
                                    listId = listId,
                                    taskId = remoteModel.id,
                                    parentId = localParent,
                                    previousId = previous,
                                )
                                ?.let {
                                    setOrderAndParent(
                                        googleTask = gtasksMetadata,
                                        task = it,
                                        local = task,
                                    )
                                }
                    } catch (e: GoogleJsonResponseException) {
                        if (e.statusCode == 400) {
                            Timber.w("HTTP 400: clearing parent and order")
                            firebase.reportException(e)
                            taskDao.setParent(0L, listOf(task.id))
                            taskDao.setOrder(task.id, 0L)
                            googleTaskDao.update(gtasksMetadata.copy(isMoved = false))
                            return task.id
                        } else {
                            throw e
                        }
                    }
                }
                // TODO: don't updateGtask if it was only moved
                try {
                    gtasksInvoker.updateGtask(listId, remoteModel)
                } catch (e: GoogleJsonResponseException) {
                    if (e.statusCode == 400 && e.details?.message == "Invalid task ID") {
                        Timber.w("HTTP 400: Invalid task ID for ${remoteModel.id}, clearing to recreate on next sync")
                        firebase.reportException(e)
                        googleTaskDao.update(
                            gtasksMetadata.copy(
                                remoteId = "",
                                isMoved = false,
                            )
                        )
                        return task.id
                    } else {
                        throw e
                    }
                }
            } catch (_: HttpNotFoundException) {
                Timber.w("HTTP 404, deleting $gtasksMetadata")
                googleTaskDao.delete(gtasksMetadata)
                return null
            }
        }
        gtasksMetadata.isMoved = false
        write(task, gtasksMetadata)
        return null
    }

    @Throws(IOException::class)
    private suspend fun fetchAndApplyRemoteChanges(
        gtasksInvoker: GtasksInvoker,
        list: CaldavCalendar
    ) {
        val listId = list.uuid
        var lastSyncDate = list.lastSync
        val tasks: MutableList<Task> = ArrayList()
        var nextPageToken: String? = null
        do {
            val taskList: Tasks = try {
                gtasksInvoker.getAllGtasksFromListId(listId, lastSyncDate + 1000L, nextPageToken)
            } catch (e: HttpNotFoundException) {
                firebase.reportException(e)
                return
            } ?: break

            val items = taskList.items
            if (items != null) {
                tasks.addAll(items)
            }
            nextPageToken = taskList.nextPageToken
        } while (!isNullOrEmpty(nextPageToken))
        Collections.sort(tasks, PARENTS_FIRST)
        for (gtask in tasks) {
            val remoteId = gtask.id
            var googleTask = googleTaskDao.getByRemoteId(remoteId)
            var task: org.tasks.data.entity.Task? = null
            if (googleTask == null) {
                googleTask = CaldavTask(
                    task = 0,
                    calendar = "",
                    remoteId = null,
                )
            } else if (googleTask.task > 0) {
                task = taskDao.fetch(googleTask.task)
            }
            gtask.updated?.let {
                lastSyncDate = max(lastSyncDate, DateTime(it).value)
            }
            val isDeleted = gtask.deleted
            val isHidden = gtask.hidden
            if (isDeleted != null && isDeleted) {
                if (task != null) {
                    taskDeleter.delete(task)
                }
                continue
            } else if (isHidden != null && isHidden) {
                if (task == null) {
                    continue
                }
                if (task.isRecurring) {
                    googleTask.remoteId = ""
                } else {
                    taskDeleter.delete(task)
                    continue
                }
            } else {
                if (task == null) {
                    task = taskCreator.createWithValues("")
                }
                setOrderAndParent(googleTask, gtask, task)
                googleTask.remoteId = gtask.id
            }
            task.title = getTruncatedValue(task.title, gtask.title, MAX_TITLE_LENGTH)
            task.completionDate = GtasksApiUtilities.gtasksCompletedTimeToUnixTime(gtask.completed?.let(::DateTime))
            val dueDate = GtasksApiUtilities.gtasksDueTimeToUnixTime(gtask.due?.let(::DateTime))
            mergeDates(createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, dueDate), task)
            task.notes = getTruncatedValue(task.notes, gtask.notes, MAX_DESCRIPTION_LENGTH)
            googleTask.calendar = listId
            if (task.title?.isNotBlank() == true || task.notes?.isNotBlank() == true) {
                write(task, googleTask)
            }
        }
        caldavDao.insertOrReplace(
            list.copy(
                lastSync = lastSyncDate
            )
        )
    }

    private suspend fun setOrderAndParent(googleTask: CaldavTask, task: Task, local: org.tasks.data.entity.Task) {
        task.position?.toLongOrNull()?.let { googleTask.remoteOrder = it }
        googleTask.remoteParent = task.parent?.takeIf { it.isNotBlank() }
        local.parent = googleTask.remoteParent?.let { googleTaskDao.getTask(it) } ?: 0L
    }

    private suspend fun write(task: org.tasks.data.entity.Task, googleTask: CaldavTask) {
        task.suppressSync()
        task.suppressRefresh()
        if (task.isNew) {
            taskDao.createNew(task)
            alarmDao.insert(task.getDefaultAlarms())
        }
        taskDao.save(task)
        googleTask
            .copy(
                task = task.id,
                lastSync = task.modificationDate,
            )
            .let {
                if (it.id == 0L) {
                    googleTaskDao.insert(it)
                } else {
                    googleTaskDao.update(it)
                }
            }
    }

    companion object {
        private const val DEFAULT_LIST = "@default" // $NON-NLS-1$
        private const val MAX_TITLE_LENGTH = 1024
        private const val MAX_DESCRIPTION_LENGTH = 8192
        private val PARENTS_FIRST = Comparator { o1: Task, o2: Task ->
            if (isNullOrEmpty(o1.parent)) {
                if (isNullOrEmpty(o2.parent)) 0 else -1
            } else {
                if (isNullOrEmpty(o2.parent)) 1 else 0
            }
        }

        fun mergeDates(remoteDueDate: Long, local: org.tasks.data.entity.Task) {
            if (remoteDueDate > 0 && local.hasDueTime()) {
                val oldDate = newDateTime(local.dueDate)
                val newDate = newDateTime(remoteDueDate)
                        .withHourOfDay(oldDate.hourOfDay)
                        .withMinuteOfHour(oldDate.minuteOfHour)
                        .withSecondOfMinute(oldDate.secondOfMinute)
                local.setDueDateAdjustingHideUntil(
                        createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY_TIME, newDate.millis))
            } else {
                local.setDueDateAdjustingHideUntil(remoteDueDate)
            }
        }

        fun truncate(string: String?, max: Int): String? =
                if (string == null || string.length <= max) string else string.substring(0, max)

        fun getTruncatedValue(currentValue: String?, newValue: String?, maxLength: Int): String? =
                if (isNullOrEmpty(newValue)
                        || newValue!!.length < maxLength || isNullOrEmpty(currentValue)
                        || !currentValue!!.startsWith(newValue)) newValue else currentValue
    }
}