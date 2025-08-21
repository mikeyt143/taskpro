/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats

import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.gcal.GCalHelper
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import org.tasks.data.createDueDate
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Task
import org.tasks.data.entity.Task.RepeatFrom
import org.tasks.data.setRecurrence
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import org.tasks.time.ONE_HOUR
import org.tasks.time.ONE_MINUTE
import org.tasks.time.ONE_WEEK
import timber.log.Timber
import java.text.ParseException
import java.util.*
import javax.inject.Inject

class RepeatTaskHelper @Inject constructor(
        private val gcalHelper: GCalHelper,
        private val alarmService: AlarmService,
        private val taskDao: TaskDao,
) {
    suspend fun handleRepeat(task: Task): Boolean {
        val recurrence = task.recurrence
        if (recurrence.isNullOrBlank()) {
            return false
        }
        val repeatAfterCompletion = task.repeatFrom == RepeatFrom.COMPLETION_DATE
        val newDueDate: Long
        val rrule: Recur
        val count: Int
        try {
            rrule = initRRule(recurrence)
            count = rrule.count
            if (count == 1) {
                return true
            }
            newDueDate = computeNextDueDate(task, recurrence, repeatAfterCompletion)
            if (newDueDate == -1L) {
                return true
            }
        } catch (e: ParseException) {
            Timber.e(e)
            return false
        }
        if (count > 1) {
            rrule.count = count - 1
            task.setRecurrence(rrule)
        }
        task.reminderLast = 0L
        task.completionDate = 0L
        val oldDueDate = task.dueDate
        task.setDueDateAdjustingHideUntil(newDueDate)
        gcalHelper.rescheduleRepeatingTask(task)
        taskDao.save(task)
        val previousDueDate = oldDueDate.takeIf { it > 0 } ?: computePreviousDueDate(task)
        rescheduleAlarms(task.id, previousDueDate, newDueDate)
        return true
    }

    suspend fun undoRepeat(task: Task, oldDueDate: Long) {
        if (task.completionDate > 0) {
            task.completionDate = 0
            taskDao.save(task)
            return
        }
        try {
            val recur = newRecur(task.recurrence!!)
            val count = recur.count
            if (count > 0) {
                recur.count = count + 1
            }
            task.setRecurrence(recur)
            val newDueDate = task.dueDate
            task.setDueDateAdjustingHideUntil(
                if (oldDueDate > 0) {
                    oldDueDate
                } else {
                    newDueDate - (computeNextDueDate(task, task.recurrence!!, false) - newDueDate)
                }
            )
            rescheduleAlarms(task.id, newDueDate, task.dueDate)
        } catch (e: ParseException) {
            Timber.e(e)
        }
        taskDao.save(task)
    }

    private suspend fun rescheduleAlarms(taskId: Long, oldDueDate: Long, newDueDate: Long) {
        if (oldDueDate <= 0 || newDueDate <= 0) {
            return
        }
        alarmService.getAlarms(taskId)
            .filter { it.type != TYPE_SNOOZE }
            .map {
                if (it.type == Alarm.TYPE_DATE_TIME) {
                    it.copy(time = it.time + newDueDate - oldDueDate)
                } else {
                    it
                }
            }
            .let { alarmService.synchronizeAlarms(taskId, it.toMutableSet()) }
    }

    companion object {
        private val weekdayCompare = Comparator { object1: WeekDay, object2: WeekDay -> WeekDay.getCalendarDay(object1) - WeekDay.getCalendarDay(object2) }

        fun computePreviousDueDate(task: Task): Long =
            task.dueDate - (computeNextDueDate(task, task.recurrence!!, task.repeatFrom == RepeatFrom.COMPLETION_DATE) - task.dueDate)

        /** Compute next due date  */
        @Throws(ParseException::class)
        fun computeNextDueDate(task: Task, recurrence: String, repeatAfterCompletion: Boolean): Long {
            val rrule = initRRule(recurrence)
            if (rrule.until != null && rrule.until is Date && task.hasDueTime()) {
                // Tasks lets you create tasks with due date-times, but recurrence until with due dates
                // This violates the spec and should be fixed in the picker
                rrule.until = DateTime.from(rrule.until).endOfDay().toDateTime()
            }

            // initialize startDateAsDV
            val original = setUpStartDate(task, repeatAfterCompletion, rrule.frequency)
            val startDateAsDV = setUpStartDateAsDV(task, original)
            return when {
                rrule.frequency == Recur.Frequency.SECONDLY ||
                rrule.frequency == Recur.Frequency.MINUTELY ||
                rrule.frequency == Recur.Frequency.HOURLY ->
                    handleSubdayRepeat(original, rrule)
                rrule.frequency == Recur.Frequency.WEEKLY && rrule.dayList.isNotEmpty() && repeatAfterCompletion ->
                    handleWeeklyRepeatAfterComplete(rrule, original, task.hasDueTime())
                rrule.frequency == Recur.Frequency.MONTHLY && rrule.dayList.isEmpty() ->
                    handleMonthlyRepeat(original, startDateAsDV, task.hasDueTime(), rrule)
                else ->
                    invokeRecurrence(rrule, original, startDateAsDV)
            }
        }

        @Deprecated("probably don't need this?")
        private fun handleWeeklyRepeatAfterComplete(
                recur: Recur, original: DateTime, hasDueTime: Boolean): Long {
            val byDay = recur.dayList
            var newDate = original.millis
            newDate += ONE_WEEK * (recur.interval.coerceAtLeast(1) - 1)
            var date = DateTime(newDate)
            Collections.sort(byDay, weekdayCompare)
            val next = findNextWeekday(byDay, date)
            do {
                date = date.plusDays(1)
            } while (date.dayOfWeek != WeekDay.getCalendarDay(next))
            val time = date.millis
            return if (hasDueTime) {
                createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time)
            } else {
                createDueDate(Task.URGENCY_SPECIFIC_DAY, time)
            }
        }

        @Deprecated("Properly support last day of month and remove this")
        private fun handleMonthlyRepeat(
                original: DateTime, startDateAsDV: Date, hasDueTime: Boolean, recur: Recur): Long {
            return if (original.isLastDayOfMonth) {
                val interval = recur.interval.coerceAtLeast(1)
                val newDateTime = original.plusMonths(interval)
                val time = newDateTime.withDayOfMonth(newDateTime.numberOfDaysInMonth).millis
                if (hasDueTime) {
                    createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time)
                } else {
                    createDueDate(Task.URGENCY_SPECIFIC_DAY, time)
                }
            } else {
                invokeRecurrence(recur, original, startDateAsDV)
            }
        }

        private fun findNextWeekday(byDay: List<WeekDay>, date: DateTime): WeekDay {
            val next = byDay[0]
            for (weekday in byDay) {
                if (WeekDay.getCalendarDay(weekday) > date.dayOfWeek) {
                    return weekday
                }
            }
            return next
        }

        private fun invokeRecurrence(recur: Recur, original: DateTime, startDateAsDV: Date): Long {
            return recur.getNextDate(startDateAsDV, startDateAsDV)
                ?.let { buildNewDueDate(original, it) }
                ?: -1
        }

        /** Compute long due date from DateValue  */
        private fun buildNewDueDate(original: DateTime, nextDate: Date): Long {
            val newDueDate: Long
            if (nextDate is net.fortuna.ical4j.model.DateTime) {
                var date = DateTime.from(nextDate)
                // time may be inaccurate due to DST, force time to be same
                date = date.withHourOfDay(original.hourOfDay).withMinuteOfHour(original.minuteOfHour)
                newDueDate = createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, date.millis)
            } else {
                newDueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY,
                        DateTime.from(nextDate).millis)
            }
            return newDueDate
        }

        /** Initialize RRule instance  */
        @Throws(ParseException::class)
        private fun initRRule(recurrence: String): Recur {
            val rrule = newRecur(recurrence)

            // handle the iCalendar "byDay" field differently depending on if
            // we are weekly or otherwise
            if (rrule.frequency != Recur.Frequency.WEEKLY && rrule.frequency != Recur.Frequency.MONTHLY) {
                rrule.dayList.clear()
            }
            return rrule
        }

        /** Set up repeat start date  */
        private fun setUpStartDate(
            task: Task, repeatAfterCompletion: Boolean, frequency: Recur.Frequency): DateTime {
            return if (repeatAfterCompletion) {
                var startDate = if (task.isCompleted) newDateTime(task.completionDate) else newDateTime()
                if (task.hasDueTime() && frequency != Recur.Frequency.HOURLY && frequency != Recur.Frequency.MINUTELY) {
                    val dueDate = newDateTime(task.dueDate)
                    startDate = startDate
                            .withHourOfDay(dueDate.hourOfDay)
                            .withMinuteOfHour(dueDate.minuteOfHour)
                            .withSecondOfMinute(dueDate.secondOfMinute)
                }
                startDate
            } else {
                if (task.hasDueDate()) newDateTime(task.dueDate) else newDateTime()
            }
        }

        private fun setUpStartDateAsDV(task: Task, startDate: DateTime): Date {
            return if (task.hasDueTime()) {
                startDate.toDateTime()
            } else {
                startDate.toDate()
            }
        }

        @Deprecated("probably don't need this?")
        private fun handleSubdayRepeat(startDate: DateTime, recur: Recur): Long {
            val millis: Long = when (recur.frequency) {
                Recur.Frequency.HOURLY -> ONE_HOUR
                Recur.Frequency.MINUTELY -> ONE_MINUTE
                else -> throw RuntimeException(
                        "Error handing subday repeat: " + recur.frequency) // $NON-NLS-1$
            }
            val newDueDate = startDate.millis + millis * recur.interval.coerceAtLeast(1)
            return createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, newDueDate)
        }

        private val Task.repeatUntil: Long
            get() = recurrence
                ?.takeIf { it.isNotBlank() }
                ?.let { newRecur(it) }
                ?.until
                ?.let { DateTime.from(it) }
                ?.millis
                ?: 0L
    }
}