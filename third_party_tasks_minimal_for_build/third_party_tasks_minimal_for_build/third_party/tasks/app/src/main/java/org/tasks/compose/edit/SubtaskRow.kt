package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.compose.CheckBox
import org.tasks.compose.ClearButton
import org.tasks.compose.DisabledText
import org.tasks.compose.SubtaskChip
import org.tasks.compose.TaskEditIcon
import org.tasks.compose.TaskEditRow
import org.tasks.data.TaskContainer
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Task
import org.tasks.data.isHidden
import org.tasks.filters.CaldavFilter
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.TasksResults
import org.tasks.tasklist.UiItem
import org.tasks.themes.TasksTheme

@Composable
fun SubtaskRow(
    originalFilter: CaldavFilter,
    filter: CaldavFilter,
    hasParent: Boolean,
    existingSubtasks: TasksResults,
    newSubtasks: List<Task>,
    openSubtask: (Task) -> Unit,
    completeExistingSubtask: (Task, Boolean) -> Unit,
    completeNewSubtask: (Task) -> Unit,
    toggleSubtask: (Long, Boolean) -> Unit,
    addSubtask: () -> Unit,
    deleteSubtask: (Task) -> Unit,
) {
    TaskEditRow(
        icon = {
            TaskEditIcon(
                id = org.tasks.R.drawable.ic_subdirectory_arrow_right_black_24dp,
                modifier = Modifier
                    .padding(
                        start = 4.dp,
                        top = 8.dp,
                        end = 8.dp,
                        bottom = 8.dp
                    )
                    .alpha(ContentAlpha.medium),
            )
        },
        content = {
            Column {
                val subtasksDisabled =
                    hasParent &&
                            !filter.isIcalendar &&
                            !originalFilter.isIcalendar &&
                            originalFilter.uuid == filter.uuid
                if (subtasksDisabled) {
                    DisabledText(
                        text = stringResource(
                            id = if (filter.isGoogleTasks) {
                                org.tasks.R.string.subtasks_multilevel_google_task
                            } else {
                                org.tasks.R.string.subtasks_multilevel_microsoft
                            }
                        ),
                        modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 20.dp, end = 16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(height = 8.dp))
                    if (existingSubtasks is TasksResults.Results) {
                        existingSubtasks
                            .tasks
                            .filterIsInstance<UiItem.Task>()
                            .map { it.task }
                            .forEach { task ->
                            ExistingSubtaskRow(
                                task = task,
                                indent = if (filter.isIcalendar) task.indent else 0,
                                onRowClick = { openSubtask(task.task) },
                                onCompleteClick = {
                                    completeExistingSubtask(
                                        task.task,
                                        !task.isCompleted
                                    )
                                },
                                onToggleSubtaskClick = { toggleSubtask(task.id, !task.isCollapsed) }
                            )
                        }
                    }
                    newSubtasks.forEach { subtask ->
                        NewSubtaskRow(
                            subtask = subtask,
                            addSubtask = addSubtask,
                            onComplete = completeNewSubtask,
                            onDelete = deleteSubtask,
                        )
                    }
                    DisabledText(
                        text = stringResource(id = org.tasks.R.string.TEA_add_subtask),
                        modifier = Modifier
                            .clickable { addSubtask() }
                            .padding(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
    )
}

@Composable
fun NewSubtaskRow(
    subtask: Task,
    addSubtask: () -> Unit,
    onComplete: (Task) -> Unit,
    onDelete: (Task) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CheckBox(
            task = subtask,
            onCompleteClick = { onComplete(subtask) },
            modifier = Modifier.align(Alignment.Top),
        )
        var text by remember(subtask.remoteId) { mutableStateOf(subtask.title ?: "") }
        val focusRequester = remember { FocusRequester() }
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                subtask.title = it
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .weight(1f)
                .focusable(enabled = true)
                .focusRequester(focusRequester)
                .alpha(if (subtask.isCompleted) ContentAlpha.disabled else ContentAlpha.high)
                .align(Alignment.Top)
                .padding(top = 12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                textDirection = TextDirection.Content,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isNotBlank()) {
                        addSubtask()
                    }
                }
            ),
            singleLine = false,
            maxLines = Int.MAX_VALUE,
        )
        ClearButton { onDelete(subtask) }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun ExistingSubtaskRow(
    task: TaskContainer, indent: Int,
    onRowClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onToggleSubtaskClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onRowClick() }
            .padding(end = 16.dp)
    ) {
        Spacer(modifier = Modifier.width((indent * 20).dp))
        CheckBox(
            task = task.task,
            onCompleteClick = onCompleteClick,
            modifier = Modifier.align(Alignment.Top),
        )
        Text(
            text = task.title!!,
            modifier = Modifier
                .weight(1f)
                .alpha(if (task.isCompleted || task.task.isHidden) ContentAlpha.disabled else ContentAlpha.high)
                .align(Alignment.Top)
                .padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = MaterialTheme.colorScheme.onSurface,
            )
        )
        if (task.hasChildren()) {
            SubtaskChip(
                collapsed = task.isCollapsed,
                children = task.children,
                compact = true,
                onClick = onToggleSubtaskClick,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoSubtasks() {
    TasksTheme {
        SubtaskRow(
            originalFilter = CaldavFilter(CaldavCalendar(), CaldavAccount()),
            filter = CaldavFilter(CaldavCalendar(), CaldavAccount()),
            hasParent = false,
            existingSubtasks = TasksResults.Results(SectionedDataSource()),
            newSubtasks = emptyList(),
            openSubtask = {},
            completeExistingSubtask = { _, _ -> },
            completeNewSubtask = {},
            toggleSubtask = { _, _ -> },
            addSubtask = {},
            deleteSubtask = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun SubtasksPreview() {
    TasksTheme {
        SubtaskRow(
            originalFilter = CaldavFilter(CaldavCalendar(), CaldavAccount()),
            filter = CaldavFilter(CaldavCalendar(), CaldavAccount()),
            hasParent = false,
            existingSubtasks = TasksResults.Results(
                SectionedDataSource(
                    tasks = listOf(
                        TaskContainer(
                            task = Task(
                                title = "Existing subtask 1",
                                priority = Task.Priority.HIGH,
                            ),
                            indent = 0
                        ),
                        TaskContainer(
                            task = Task(
                                title = "Existing subtask 2 with a really long title",
                                priority = Task.Priority.LOW,
                            ),
                            indent = 1
                        )
                    )
                )
            ),
            newSubtasks = listOf(
                Task().apply {
                    title = "New subtask 1"
                },
                Task().apply {
                    title = "New subtask 2 with a really long title"
                },
                Task(),
            ),
            openSubtask = {},
            completeExistingSubtask = { _, _ -> },
            completeNewSubtask = {},
            toggleSubtask = { _, _ -> },
            addSubtask = {},
            deleteSubtask = {},
        )
    }
}