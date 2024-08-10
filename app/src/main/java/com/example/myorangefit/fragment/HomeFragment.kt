package com.example.myorangefit.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.myorangefit.R
import com.example.myorangefit.databinding.CalendarDayBinding
import com.example.myorangefit.databinding.CalendarDayWeekBinding
import com.example.myorangefit.databinding.FragmentHomeBinding
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.WeekDayPosition
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekCalendarView
import com.kizitonwose.calendar.view.WeekDayBinder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth


class HomeFragment : Fragment() {

    // ViewBinding
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var contx: Context

    private lateinit var weekCalendarView: WeekCalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        weekCalendarView = binding.exOneWeekCalendar

        contx = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val daysOfWeek = daysOfWeek()
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(1)
        val endMonth = currentMonth.plusMonths(1)
        setupWeekCalendar(startMonth, endMonth, currentMonth, daysOfWeek)

        weekCalendarView.scrollToWeek(LocalDate.now())
        weekCalendarView.scrollPaged = false

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupWeekCalendar(
        startMonth: YearMonth,
        endMonth: YearMonth,
        currentMonth: YearMonth,
        daysOfWeek: List<DayOfWeek>,
    ) {
        class WeekDayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: WeekDay
            val binding = CalendarDayBinding.bind(view)
            val number: TextView = binding.exOneDayText
            //val week: TextView = binding.week
            val dayContainer: LinearLayout = binding.dayContainer

            init {

            }
        }

        weekCalendarView.dayBinder = object : WeekDayBinder<WeekDayViewContainer> {
            override fun create(view: View): WeekDayViewContainer = WeekDayViewContainer(view)
            override fun bind(container: WeekDayViewContainer, data: WeekDay) {
                container.day = data
                bindDate(data.date, container.dayContainer, container.number,  data.position == WeekDayPosition.RangeDate)
            }
        }

        weekCalendarView.setup(
            startMonth.atStartOfMonth(),
            endMonth.atEndOfMonth(),
            daysOfWeek.first(),
        )
        weekCalendarView.scrollToWeek(currentMonth.atStartOfMonth())
    }

    @SuppressLint("SetTextI18n")
    private fun bindDate(date: LocalDate, dayContainer: LinearLayout, number: TextView,  isSelectable: Boolean) {
        number.text = "${date.dayOfMonth}"
        //week.text = date.dayOfWeek.name.substring(0, 1) + date.dayOfWeek.name.substring(1, 3).lowercase()
        if (isSelectable) {
            if (date.dayOfWeek == DayOfWeek.MONDAY) {
                dayContainer.setBackgroundResource(R.drawable.selected_bg)
                number.setTextColor(resources.getColor(R.color.white))
                //week.setTextColor(resources.getColor(R.color.white))
            } else {
                number.setTextColor(resources.getColor(R.color.gray))
                //week.setTextColor(resources.getColor(R.color.gray))
                dayContainer.setBackgroundResource(R.drawable.day_bg)
            }
        } else {
            number.setTextColor(resources.getColor(R.color.gray))
            //week.setTextColor(resources.getColor(R.color.gray))
            dayContainer.background = null
        }
    }

}