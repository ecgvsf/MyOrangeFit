package com.example.myorangefit

import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class EventDecorator(
    private val dates: Set<CalendarDay>,
    private val colors: List<Int>
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        colors.forEachIndexed { index, color ->
            view.addSpan(DotSpan(color, colors.size, index))
        }
    }
}
