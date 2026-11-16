package com.mg.bubumeal.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** 월요일에 시작해 일요일에 끝나는 한 주. */
data class Week(val start: LocalDate) {
    init {
        require(start.dayOfWeek == DayOfWeek.MONDAY) { "주는 월요일에 시작해야 한다: $start" }
    }

    val end: LocalDate get() = start.plus(6, DateTimeUnit.DAY)

    val days: List<LocalDate> get() = (0..6).map { start.plus(it, DateTimeUnit.DAY) }

    fun previous(): Week = Week(start.minus(1, DateTimeUnit.WEEK))

    fun next(): Week = Week(start.plus(1, DateTimeUnit.WEEK))

    operator fun contains(date: LocalDate): Boolean = date in start..end

    companion object {
        fun containing(date: LocalDate): Week =
            Week(date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY))

        /** "이번 주". 시간대에 따라 날짜가 달라지므로 시간대를 바꿀 수 있게 받는다. 기본값은 기기 시간대(임시 결정). */
        fun current(
            clock: Clock = Clock.System,
            timeZone: TimeZone = TimeZone.currentSystemDefault(),
        ): Week = containing(clock.todayIn(timeZone))
    }
}
