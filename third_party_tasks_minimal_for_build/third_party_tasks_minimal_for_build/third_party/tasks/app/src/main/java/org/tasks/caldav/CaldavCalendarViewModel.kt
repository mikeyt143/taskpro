package org.tasks.caldav

import android.content.Intent
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.data.PrincipalWithAccess
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.PrincipalDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_UNKNOWN
import org.tasks.filters.CaldavFilter
import org.tasks.sync.SyncAdapters
import javax.inject.Inject

@HiltViewModel
class CaldavCalendarViewModel @Inject constructor(
    private val provider: CaldavClientProvider,
    private val caldavDao: CaldavDao,
    private val principalDao: PrincipalDao,
    private val taskDeleter: TaskDeleter,
    private val syncAdapters: SyncAdapters,
) : CaldavViewModel() {
    var ignoreFinish = false

    suspend fun createCalendar(
        caldavAccount: CaldavAccount,
        name: String,
        color: Int,
        icon: String?
    ): CaldavCalendar? =
        doRequest {
            val url = withContext(Dispatchers.IO) {
                provider.forAccount(caldavAccount).makeCollection(name, color, icon)
            }
            val calendar = CaldavCalendar(
                uuid = UUIDHelper.newUUID(),
                account = caldavAccount.uuid,
                url = url,
                name = name,
                color = color,
                icon = icon,
            ).apply {
                caldavDao.insert(this)
            }
            if (!ignoreFinish) {
                finish.value = Intent().putExtra(
                    MainActivity.OPEN_FILTER,
                    CaldavFilter(calendar = calendar, account = caldavAccount)
                )
            }
            calendar
        }

    suspend fun updateCalendar(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        name: String,
        color: Int,
        icon: String?
    ) =
        doRequest {
            withContext(Dispatchers.IO) {
                provider.forAccount(account, calendar.url!!).updateCollection(name, color, icon)
            }
            val result = calendar.copy(
                name = name,
                color = color,
                icon = icon,
            )
            caldavDao.update(result)
            finish.value = Intent(TaskListFragment.ACTION_RELOAD)
                .putExtra(
                    MainActivity.OPEN_FILTER,
                    CaldavFilter(calendar = result, account = account)
                )
        }

    suspend fun deleteCalendar(account: CaldavAccount, calendar: CaldavCalendar) =
        doRequest {
            withContext(Dispatchers.IO) {
                provider.forAccount(account, calendar.url!!).deleteCollection()
            }
            taskDeleter.delete(calendar)
            finish.value = Intent(TaskListFragment.ACTION_DELETED)
        }

    suspend fun addUser(
        account: CaldavAccount,
        list: CaldavCalendar,
        input: String
    ) = doRequest {
        val href = if (account.serverType == CaldavAccount.SERVER_OWNCLOUD)
            "principal:principals/users/$input"
        else
            "mailto:$input"
        withContext(Dispatchers.IO) {
            provider.forAccount(account, list.url!!).share(account, href)
        }
        val principal = principalDao.getOrCreatePrincipal(account, href)
        principalDao.getOrCreateAccess(list, principal, INVITE_UNKNOWN, ACCESS_READ_WRITE)
        syncAdapters.sync(true)
    }

    suspend fun removeUser(account: CaldavAccount, list: CaldavCalendar, principal: PrincipalWithAccess) =
        doRequest {
            withContext(Dispatchers.IO) {
                provider.forAccount(account).removePrincipal(account, list, principal.href)
            }
            principalDao.delete(principal.access)
        }
}