package org.tasks.data

import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.core.SortHelper
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.db.Table
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion
import org.tasks.data.sql.Join
import org.tasks.data.sql.QueryTemplate
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.preferences.QueryPreferences

internal object TaskListQueryRecursive {
    private val RECURSIVE = Table("recursive_tasks")

    fun getRecursiveQuery(
        filter: Filter,
        preferences: QueryPreferences,
    ): String {
        val parentQuery = when (filter) {
            is CaldavFilter -> newCaldavQuery(filter.uuid)
            else -> PermaSql.replacePlaceholdersForQuery(filter.sql!!)
        }
        val manualSort = preferences.isManualSort
        val groupPreference = preferences.groupMode
        val groupMode = when {
            filter is CaldavFilter && (manualSort || groupPreference == SortHelper.SORT_LIST) ->
                SortHelper.GROUP_NONE
            else -> groupPreference
        }
        val sortMode = when {
            !manualSort || filter !is CaldavFilter -> preferences.sortMode
            filter.isGoogleTasks -> SortHelper.SORT_GTASKS
            else -> SortHelper.SORT_CALDAV
        }
        val subtaskPreference = preferences.subtaskMode
        val subtaskMode = when {
            sortMode == SortHelper.SORT_GTASKS || sortMode == SortHelper.SORT_CALDAV -> sortMode
            subtaskPreference == SortHelper.SORT_MANUAL -> SortHelper.SORT_CALDAV
            else -> subtaskPreference
        }
        val completedMode = preferences.completedMode
        val groupAscending =
            preferences.groupAscending && groupMode != SortHelper.GROUP_NONE
        val sortAscending =
            preferences.sortAscending && sortMode != SortHelper.SORT_GTASKS && sortMode != SortHelper.SORT_CALDAV
        val subtaskAscending =
            preferences.subtaskAscending && subtaskMode != SortHelper.SORT_GTASKS && subtaskMode != SortHelper.SORT_CALDAV
        val completedAtBottom = preferences.completedTasksAtBottom
        val parentCompleted = if (completedAtBottom) "tasks.completed > 0" else "0"
        val completionSort = if (completedAtBottom) {
            "(CASE WHEN tasks.completed > 0 THEN ${SortHelper.orderSelectForSortTypeRecursive(completedMode, false)} ELSE 0 END)"
        } else {
            "0"
        }
        val query = """
            WITH RECURSIVE recursive_tasks AS (
                SELECT 
                    tasks._id AS task,
                    $parentCompleted AS parent_complete,
                    $completionSort AS completion_sort,
                    0 AS parent,
                    tasks.collapsed AS collapsed,
                    0 AS hidden,
                    0 AS indent,
                    UPPER(tasks.title) AS sort_title,
                    ${SortHelper.orderSelectForSortTypeRecursive(groupMode, true)} AS primary_group,
                    ${SortHelper.orderSelectForSortTypeRecursive(sortMode, false)} AS primary_sort,
                    NULL as secondary_sort,
                    ${SortHelper.getSortGroup(groupMode)} AS sort_group,
                    '/' || tasks._id || '/' as recursive_path
                FROM tasks
                ${
                    if (groupMode == SortHelper.SORT_LIST) {
                        """
                            INNER JOIN caldav_tasks ON cd_task = tasks._id AND cd_deleted = 0
                            INNER JOIN caldav_lists ON cd_calendar = cdl_uuid
                        """.trimIndent()
                    } else {
                        ""
                    }
                }
                $parentQuery
                UNION ALL SELECT
                    tasks._id AS task,
                    $parentCompleted AS parent_complete,
                    $completionSort AS completion_sort,
                    recursive_tasks.task AS parent,
                    tasks.collapsed AS collapsed,
                    CASE WHEN recursive_tasks.collapsed > 0 OR recursive_tasks.hidden > 0 THEN 1 ELSE 0 END AS hidden,
                    CASE 
                        WHEN $parentCompleted AND recursive_tasks.parent_complete = 0 THEN 0
                        ELSE recursive_tasks.indent + 1 
                    END AS indent,
                    UPPER(tasks.title) AS sort_title,
                    recursive_tasks.primary_group AS primary_group,
                    recursive_tasks.primary_sort AS primary_sort,
                    ${SortHelper.orderSelectForSortTypeRecursive(subtaskMode, false)} AS secondary_sort,
                    recursive_tasks.sort_group AS sort_group,
                    recursive_tasks.recursive_path || tasks._id || '/' AS recursive_path
                FROM tasks
                INNER JOIN recursive_tasks ON tasks.parent = recursive_tasks.task
                WHERE
                    ${activeAndVisible()}
                    AND recursive_tasks.recursive_path NOT LIKE '%/' || tasks._id || '/%'
                ORDER BY
                    parent_complete,
                    indent DESC,
                    completion_sort ${if (preferences.completedAscending) "" else "DESC"},
                    ${SortHelper.orderForGroupTypeRecursive(groupMode, groupAscending)},
                    ${SortHelper.orderForSortTypeRecursive(sortMode, sortAscending, subtaskMode, subtaskAscending)}
            ),
            max_indent AS (
                SELECT
                    *,
                    MAX(recursive_tasks.indent) OVER (PARTITION BY task) AS max_indent,
                    ROW_NUMBER() OVER () AS sequence
                FROM recursive_tasks
            ),
            descendants_recursive AS (
                SELECT
                    parent,
                    task as descendant,
                    parent_complete as completed
                FROM recursive_tasks
                WHERE parent > 0
                UNION ALL
                SELECT
                    d.parent,
                    r.task as descendant,
                    r.parent_complete as completed
                FROM recursive_tasks r
                    JOIN descendants_recursive d ON r.parent = d.descendant
            ),
            descendants AS (
                SELECT
                    parent,
                    COUNT(DISTINCT CASE WHEN completed > 0 THEN descendant ELSE NULL END) as completed_children,
                    COUNT(DISTINCT CASE WHEN completed = 0 THEN descendant ELSE NULL END) as uncompleted_children
                FROM descendants_recursive
                GROUP BY parent
            )
            SELECT
                ${TaskListQuery.FIELDS.joinToString(",\n") { it.toStringInSelect() }},
                group_concat(distinct(tag_uid)) AS tags,
                indent,
                sort_group,
                CASE
                    WHEN parent_complete > 0 THEN completed_children
                    ELSE uncompleted_children
                END as children,
                primary_sort,
                secondary_sort,
                parent_complete
            FROM tasks
                INNER JOIN max_indent
                    ON tasks._id = max_indent.task
                    AND indent = max_indent
                    AND hidden = 0
                LEFT JOIN descendants ON descendants.parent = tasks._id
                LEFT JOIN tags ON tags.task = tasks._id
                ${TaskListQuery.JOINS}
            GROUP BY tasks._id
            ORDER BY sequence
        """.trimIndent()

        return SortHelper.adjustQueryForFlags(preferences, query)
    }

    private fun newCaldavQuery(list: String) =
            QueryTemplate()
                    .join(
                        Join.inner(
                            CaldavTask.TABLE,
                            Criterion.and(
                                    CaldavTask.CALENDAR.eq(list),
                                    CaldavTask.TASK.eq(Task.ID),
                                    CaldavTask.DELETED.eq(0))))
                    .where(Criterion.and(activeAndVisible(), Task.PARENT.eq(0)))
                    .toString()
}
