package org.tasks.sync.microsoft

import com.natpryce.makeiteasy.MakeItEasy
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TestUtilities
import org.tasks.TestUtilities.withTZ
import org.tasks.data.entity.Task
import org.tasks.makers.TaskMaker
import org.tasks.time.DateTime

class ConvertFromMicrosoftTests {
    @Test
    fun titleFromRemote() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task.json")
        assertEquals("Basic task", local.title)
    }

    @Test
    fun useNullForBlankBody() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task.json")
        Assert.assertNull(local.notes)
    }

    @Test
    fun parseDescription() {
        val (local, _) = TestUtilities.mstodo("microsoft/task_with_description.json")
        assertEquals("Hello world\r\n", local.notes)
    }

    @Test
    fun keepPriority() {
        val (local, _) = TestUtilities.mstodo(
            "microsoft/basic_task.json",
            task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.MEDIUM)),
            defaultPriority = Task.Priority.LOW
        )
        assertEquals(Task.Priority.MEDIUM, local.priority)
    }

    @Test
    fun useDefaultPriority() {
        val (local, _) = TestUtilities.mstodo(
            "microsoft/basic_task.json",
            task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH)),
            defaultPriority = Task.Priority.LOW
        )
        assertEquals(Task.Priority.LOW, local.priority)
    }

    @Test
    fun noPriorityWhenDefaultIsHigh() {
        val (local, _) = TestUtilities.mstodo(
            "microsoft/basic_task.json",
            task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH)),
            defaultPriority = Task.Priority.HIGH
        )
        assertEquals(Task.Priority.NONE, local.priority)
    }

    @Test
    fun noCompletionDate() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task.json")
        assertEquals(0, local.completionDate)
    }

    @Test
    fun parseCompletionDate() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/completed_task.json")
            assertEquals(DateTime(2022, 9, 18, 0, 0).millis, local.completionDate)
        }
    }

    @Test
    fun parseDueDate() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/basic_task_with_due_date.json")
            assertEquals(DateTime(2023, 7, 19, 0, 0).millis, local.dueDate)
        }
    }

    @Test
    fun parseCreationDate() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/basic_task_with_due_date.json")
            assertEquals(
                DateTime(2023, 7, 19, 23, 20, 56, 9).millis,
                local.creationDate
            )
        }
    }

    @Test
    fun parseModificationDate() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/basic_task_with_due_date.json")
            assertEquals(
                DateTime(2023, 7, 19, 23, 21, 6, 269).millis,
                local.modificationDate
            )
        }
    }

    @Test
    fun parseDailyRecurrence() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/repeat_daily.json")
            assertEquals("FREQ=DAILY", local.recurrence)
        }
    }

    @Test
    fun parseWeekdayRecurrence() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/repeat_weekdays.json")
            assertEquals("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,TU,WE,TH,FR", local.recurrence)
        }
    }

    @Test
    fun parseAbsoluteMonthlyRecurrence() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/repeat_monthly.json")
            assertEquals("FREQ=MONTHLY", local.recurrence)
        }
    }

    @Test
    fun parseAbsoluteYearlyRecurrence() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/repeat_yearly.json")
            assertEquals("FREQ=YEARLY", local.recurrence)
        }
    }
}
