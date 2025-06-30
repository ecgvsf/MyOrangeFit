package com.myorangefit.app.fragment

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EdgeEffect
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import android.transition.Fade
import androidx.viewpager2.widget.ViewPager2
import com.myorangefit.app.activity.exercise.ManageWorkoutActivity
import com.myorangefit.app.adapter.ExercisePagerAdapter
import com.myorangefit.app.async.WorkoutViewModel
import com.myorangefit.app.database.DatabaseHelper
import com.myorangefit.app.database.DatabaseHelperSingleton
import com.myorangefit.app.databinding.CalendarDayBinding
import com.myorangefit.app.databinding.FragmentCalendarBinding
import com.myorangefit.app.model.Workout
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import com.myorangefit.app.R

class CalendarFragment : Fragment() {

    private lateinit var today: LocalDate
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var viewModel: WorkoutViewModel
    private lateinit var dateBodyPartMap: MutableMap<LocalDate, MutableSet<String>>

    private lateinit var viewPager: ViewPager2
    private var nItems = 4

    // ViewBinding
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var monthCalendarView: CalendarView
    private lateinit var weekCalendarView: WeekCalendarView
    private lateinit var cardView: CardView

    private var selectedDate: LocalDate? = null
    private var isWeekMode = false
    var isSelectionMode: Boolean = false

    private lateinit var numDayTextView: TextView
    private lateinit var weekDayTextView: TextView

    private var expandedPosition = 0f
    private var collapsedPosition = 0f

    private lateinit var contx: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade()
        exitTransition  = Fade()

        today = (arguments?.getSerializable("today") as? LocalDate)!!
        contx = requireContext()
        databaseHelper = DatabaseHelperSingleton.getInstance(contx)
        viewModel = ViewModelProvider(requireActivity())[WorkoutViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        monthCalendarView = binding.exOneCalendar
        weekCalendarView = binding.exOneWeekCalendar
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = binding.pager

        // Osserva eventuali cambiamenti sul numero di item (senza forzare il valore)
        viewModel.nItems.observe(viewLifecycleOwner) { num ->
            nItems = num
            Log.e("sss", "$nItems, $num")
            refreshPagerAdapter()
        }

        viewModel.allWorkouts.observe(viewLifecycleOwner) { workoutMap ->
            dateBodyPartMap = workoutMap
        }
        viewModel.selectedData.observe(viewLifecycleOwner) { selected ->
            selectedDate = selected
            viewModel.loadWorkoutsForDate(selected)
        }
        // All’interno del observer per viewModel.workoutsForDate:
        viewModel.workoutsForDate.observe(viewLifecycleOwner) { workoutsForDate ->
            val selectedDate = viewModel.selectedData.value
            val workouts = workoutsForDate[selectedDate] ?: emptyList()

            displayWorkouts(workouts.toMutableList())
            updateTitle()
            selectedDate?.let { updateDayInfo(it) }

            // Calcola il chunk size in modo sicuro
            val chunkSize = if (nItems > 0) nItems else 1
            val exerciseData = workouts.chunked(chunkSize).map { it.toMutableList() }.toMutableList()
            val d = selectedDate ?: today

            // Crea l’adapter per il ViewPager2
            val exercisePagerAdapter = ExercisePagerAdapter(requireActivity(), exerciseData, d, isWeekMode)
            viewPager.adapter = exercisePagerAdapter

            // Imposta il limite delle pagine fuori schermo se ci sono allenamenti
            if (workouts.isNotEmpty())
                viewPager.setOffscreenPageLimit(exerciseData.size)
        }


        val daysOfWeek = daysOfWeek()

        numDayTextView = binding.numDay
        weekDayTextView = binding.weekDay

        cardView = binding.bottomSheet
        // Imposta l’altezza della cardView pari all’altezza dello schermo
        val screenHeight = resources.displayMetrics.heightPixels
        cardView.updateLayoutParams {
            //height = screenHeight
        }
        cardView.updateLayoutParams<RelativeLayout.LayoutParams> {
            //bottomMargin = -90
        }

        binding.lineContainer.setOnTouchListener(cardViewTouchListener)

        binding.legendLayout.root.children.forEachIndexed { index, view ->
            (view as TextView).apply {
                text = daysOfWeek[index].displayText()
                setTextColor(Color.WHITE)
            }
        }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(60)
        val endMonth = currentMonth.plusMonths(3)
        setupMonthCalendar(startMonth, endMonth, currentMonth, daysOfWeek)
        setupWeekCalendar(startMonth, endMonth, currentMonth, daysOfWeek)

        monthCalendarView.isInvisible = isWeekMode
        weekCalendarView.isInvisible = !isWeekMode

        if (viewModel.selectedData.value?.month != today.month) {
            viewModel.selectedData.value?.let { scroll(it) }
        }

        cardView.post {
            //updateCardView()
            val layoutParams = cardView.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin = 16.dpToPx(requireContext())
            cardView.layoutParams = layoutParams
            adjustPagerHeight()
        }

        Log.d("dataaaa", "$screenHeight, ${cardView.layoutParams.height}")

        monthCalendarView.viewTreeObserver.addOnGlobalLayoutListener {
            collapsedPosition = monthCalendarView.bottom.toFloat()
            //if (isWeekMode)
                //cardView.y = collapsedPosition
        }

        binding.manageWorkoutsButton.setOnClickListener {
            startActivity(Intent(contx, ManageWorkoutActivity::class.java))
        }

        binding.selectButton.setOnClickListener {
            viewModel.workoutsForDate.value?.let { map ->
                val workouts = map[selectedDate] ?: emptyList()
                if (workouts.isNotEmpty())
                    selectionMode()
            }
        }

        binding.dayLayout.setOnClickListener { scrollToday() }
        binding.left.setOnClickListener { scroll(-1) }
        binding.right.setOnClickListener { scroll(1) }

        binding.trashButton.setOnClickListener {
            (viewPager.adapter as? ExercisePagerAdapter)?.let { adapter ->
                val empty = adapter.removeAndShiftItem()
                if (empty) {
                    displayWorkouts(mutableListOf())
                    viewModel.getStreak()
                    viewModel.getWeekWorkout()
                    viewModel.getWeekDates(today)
                }
                viewModel.reloadDayAllWorkout(selectedDate!!)
                viewModel.allWorkouts.observe(viewLifecycleOwner) {
                    monthCalendarView.notifyDateChanged(selectedDate!!)
                    weekCalendarView.notifyDateChanged(selectedDate!!)
                }
            }
            selectionMode()
        }

        val recyclerView = viewPager.getChildAt(0) as RecyclerView
        // 1) disabiliti l’overscroll “stretch/glow” via overScrollMode
        recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        // 2) oppure (oppure in aggiunta) imposti un EdgeEffectFactory “muto”
        recyclerView.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return object : EdgeEffect(view.context) {
                    override fun onPull(deltaDistance: Float) {}
                    override fun onPull(deltaDistance: Float, displacement: Float) {}
                    override fun onRelease() {}
                    override fun onAbsorb(velocity: Int) {}
                }
            }
        }

        // 2) Osserva nItems e fai refresh ogni volta
        viewModel.nItems.observe(viewLifecycleOwner) { newCount ->
            nItems = newCount

        }

        // Permetti ai figli di uscire dai bordi
        viewPager.clipToPadding = false
        viewPager.clipChildren = false
        // Anche la RecyclerView interna
        val rv = viewPager.getChildAt(0) as RecyclerView
        rv.clipToPadding = false
        rv.clipChildren = false

        // Disabilita clipping anche sul CardView e sul suo contenitore
        cardView.clipToPadding = false
        cardView.clipChildren = false
        (binding.bottomSheet.getChildAt(0) as ViewGroup).clipChildren = false
    }

    private fun refreshPagerAdapter() {
        val date = selectedDate ?: today
        val workouts = viewModel.workoutsForDate.value?.get(date) ?: emptyList()
        val chunkSize = nItems.coerceAtLeast(1)
        val pages = workouts.chunked(chunkSize).map { it.toMutableList() }
        val adapter = ExercisePagerAdapter(requireActivity(), pages.toMutableList(), date, isWeekMode)
        viewPager.adapter = adapter
        if (pages.isNotEmpty()) {
            viewPager.offscreenPageLimit = pages.size
        }
    }

    /* ---- Funzioni per lo scroll del calendario ---- */
    private fun scrollToday() {
        monthCalendarView.scrollToMonth(today.yearMonth)
        weekCalendarView.scrollToWeek(today)
        dateClicked(today)
    }

    private fun scroll(date: LocalDate) {
        monthCalendarView.scrollToMonth(date.yearMonth)
        weekCalendarView.scrollToWeek(date)
        dateClicked(date)
    }

    private fun scroll(direction: Long) {
        if (!isWeekMode) {
            monthCalendarView.findFirstVisibleMonth()?.yearMonth?.plusMonths(direction)?.let {
                monthCalendarView.scrollToMonth(it)
            }
        } else {
            weekCalendarView.findFirstVisibleWeek()?.days?.first()?.date?.plusWeeks(direction)?.let {
                weekCalendarView.scrollToWeek(it)
            }
        }
    }

    /* ---- Aggiornamento info del giorno ---- */
    @SuppressLint("SetTextI18n")
    private fun updateDayInfo(date: LocalDate) {
        numDayTextView.text = date.dayOfMonth.toString()
        weekDayTextView.text = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            .substring(0, 3).lowercase()
    }

    /* ---- Animazione del calendario e della CardView ---- */
    private val cardViewTouchListener = object : View.OnTouchListener {
        private var initialTouchY: Float = 0f
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> return true
                MotionEvent.ACTION_UP -> {
                    val deltaY = event.rawY - initialTouchY
                    val newMode = if (deltaY < 0) true else if (deltaY > 0) false else return false

                    if (newMode != isWeekMode) {
                        isWeekMode = newMode
                        binding.selectButton.visibility = if (isWeekMode) View.VISIBLE else View.GONE
                        animateCalendar()
                        updateTitle()
                    }
                    return true
                }
            }
            return false
        }
    }

    private fun animateCalendar() {
        val layoutParams = cardView.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = 0.dpToPx(requireContext())

        val dateToShow = selectedDate ?: today
        if (!isWeekMode)
            monthCalendarView.scrollToMonth(dateToShow.yearMonth)
        else
            weekCalendarView.scrollToWeek(dateToShow.startOfWeek())

        val weekHeight = weekCalendarView.height
        val monthDaysCount = monthCalendarView.findFirstVisibleMonth()?.weekDays.orEmpty().count()
        val visibleMonthHeight = weekHeight * monthDaysCount + 32
        val adjustedWeekHeight = weekHeight + 32

        val oldHeight = if (isWeekMode) visibleMonthHeight else adjustedWeekHeight
        val newHeight = if (isWeekMode) adjustedWeekHeight else visibleMonthHeight

        val oldPosition = cardView.y
        val newPosition = if (isWeekMode) oldHeight else newHeight + 32

        val line = binding.line
        val oldLineWidth = line.layoutParams.width
        val newLineWidth = if (isWeekMode) oldLineWidth + 64 else oldLineWidth - 64

        val heightAnimator = ValueAnimator.ofInt(oldHeight, newHeight).apply {
            addUpdateListener { anim ->
                val animatedHeight = anim.animatedValue as Int
                monthCalendarView.updateLayoutParams {
                    height = animatedHeight
                }
                monthCalendarView.children.forEach { it.requestLayout() }
                //cardView.y = (oldPosition + (newPosition - oldPosition) * (animatedHeight.toFloat() / newHeight))
            }
            doOnStart {
                //adjustPagerHeight(getHeight((weekCalendarView.y + newHeight).toFloat()))
                if (!isWeekMode) {
                    weekCalendarView.isInvisible = true
                    monthCalendarView.isVisible = true
                }
                (viewPager.adapter as? ExercisePagerAdapter)?.let { adapter ->
                    for (i in 0 until adapter.itemCount) {
                        adapter.toggleExpansion(i, isWeekMode)
                    }
                }
                //viewPager.updateLayoutParams { height = MATCH_PARENT }
            }
            doOnEnd {
                if (isWeekMode) {
                    weekCalendarView.isVisible = true
                    monthCalendarView.isInvisible = true
                } else {
                    //monthCalendarView.updateLayoutParams { height = WRAP_CONTENT }
                }

                //updateCardView()
                adjustPagerHeight()
                updateTitle()
            }
            duration = 300
        }

        val lineWidthAnimator = ValueAnimator.ofInt(oldLineWidth, newLineWidth).apply {
            addUpdateListener { anim ->
                val animatedWidth = anim.animatedValue as Int
                line.layoutParams.width = animatedWidth
                line.requestLayout()
            }
            duration = 300
        }

        AnimatorSet().apply {
            playTogether(heightAnimator, lineWidthAnimator)
            start()
        }
        Log.d("dataaaaa", "${cardView.height}")
    }

    private fun updateCardView() {
        val offset = 32f
        collapsedPosition = monthCalendarView.bottom.toFloat() + offset
        expandedPosition  = weekCalendarView.bottom.toFloat() + offset
        // 3) Riposiziona la CardView
        cardView.y = if (isWeekMode) expandedPosition else collapsedPosition
    }

    /* ---- Modalità di selezione ---- */
    private fun selectionMode() {
        (viewPager.adapter as? ExercisePagerAdapter)?.let { adapter ->
            if (isSelectionMode) {
                adapter.setSwipe(true)
                binding.trashButton.visibility = View.GONE
            } else {
                adapter.setSwipe(false)
                binding.trashButton.visibility = View.VISIBLE
            }
            for (i in 0 until adapter.itemCount)
                adapter.toggleSelection(i, isSelectionMode)
            isSelectionMode = !isSelectionMode
        }
    }

    private fun LocalDate.startOfWeek(): LocalDate {
        return this.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    /* ---- Inizializzazione dei calendari ---- */
    private fun setupMonthCalendar(
        startMonth: YearMonth,
        endMonth: YearMonth,
        currentMonth: YearMonth,
        daysOfWeek: List<DayOfWeek>
    ) {
        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay
            val textView = CalendarDayBinding.bind(view).exOneDayText
            val dotContainer = CalendarDayBinding.bind(view).dotContainer
            val dayContainer = CalendarDayBinding.bind(view).dayContainer
            init {
                view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate) {
                        dateClicked(day.date)
                    }
                }
            }
        }

        monthCalendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                bindDate(data.date, container.dayContainer, container.dotContainer, container.textView, data.position == DayPosition.MonthDate)
            }
        }
        monthCalendarView.monthScrollListener = { updateTitle() }
        monthCalendarView.setup(startMonth, endMonth, daysOfWeek.first())
        monthCalendarView.scrollToMonth(currentMonth)
    }

    private fun setupWeekCalendar(
        startMonth: YearMonth,
        endMonth: YearMonth,
        currentMonth: YearMonth,
        daysOfWeek: List<DayOfWeek>
    ) {
        class WeekDayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: WeekDay
            val textView = CalendarDayBinding.bind(view).exOneDayText
            val dotContainer = CalendarDayBinding.bind(view).dotContainer
            val dayContainer = CalendarDayBinding.bind(view).dayContainer
            init {
                view.setOnClickListener {
                    if (day.position == WeekDayPosition.RangeDate) {
                        dateClicked(day.date)
                    }
                }
            }
        }
        weekCalendarView.dayBinder = object : WeekDayBinder<WeekDayViewContainer> {
            override fun create(view: View): WeekDayViewContainer = WeekDayViewContainer(view)
            override fun bind(container: WeekDayViewContainer, data: WeekDay) {
                container.day = data
                bindDate(data.date, container.dayContainer, container.dotContainer, container.textView, data.position == WeekDayPosition.RangeDate)
            }
        }
        weekCalendarView.weekScrollListener = { updateTitle() }
        weekCalendarView.setup(
            startMonth.atStartOfMonth(),
            endMonth.atEndOfMonth(),
            daysOfWeek.first()
        )
        weekCalendarView.scrollToWeek(currentMonth.atStartOfMonth())
    }

    private fun bindDate(date: LocalDate, dayContainer: LinearLayout, dotContainer: LinearLayout, textView: TextView, isSelectable: Boolean) {
        dotContainer.removeAllViews()
        textView.text = date.dayOfMonth.toString()
        if (isSelectable) {
            when {
                selectedDate == date -> {
                    dayContainer.setBackgroundResource(R.drawable.selected_bg)
                    textView.setTextColor(Color.WHITE)
                }
                today == date -> {
                    dayContainer.setBackgroundResource(R.drawable.today_bg)
                    textView.setTextColor(Color.WHITE)
                }
                else -> {
                    textView.setTextColor(Color.WHITE)
                    dayContainer.setBackgroundResource(R.drawable.day_bg)
                }
            }
            val colorMap = mapOf(
                "1" to ContextCompat.getColor(contx, R.color.chestColor),
                "2" to ContextCompat.getColor(contx, R.color.backColor),
                "3" to ContextCompat.getColor(contx, R.color.legsColor),
                "4" to ContextCompat.getColor(contx, R.color.armsColor),
                "5" to ContextCompat.getColor(contx, R.color.shouldersColor),
                "6" to ContextCompat.getColor(contx, R.color.absColor)
            )
            val bodyParts = dateBodyPartMap[date] ?: emptySet()
            bodyParts.forEach { bodyPart ->
                colorMap[bodyPart]?.let { color ->
                    val dotView = View(dotContainer.context).apply {
                        layoutParams = LinearLayout.LayoutParams(16, 16).apply { marginEnd = 4 }
                        background = ContextCompat.getDrawable(context, R.drawable.circle)
                        background.setTint(color)
                    }
                    dotContainer.addView(dotView)
                }
            }
        } else {
            textView.setTextColor(Color.GRAY)
            dayContainer.background = null
        }
    }

    private fun dateClicked(date: LocalDate) {
        if (date == selectedDate) return
        isSelectionMode = false
        binding.trashButton.visibility = View.GONE
        val previousDate = selectedDate
        viewModel.updateData(date)
        previousDate?.let {
            monthCalendarView.notifyDateChanged(it)
            weekCalendarView.notifyDateChanged(it)
            Log.e("sss", "changing $it to $date")
        }
        monthCalendarView.notifyDateChanged(date)
        weekCalendarView.notifyDateChanged(date)
    }

    /* ---- Caricamento degli allenamenti ---- */
    private fun displayWorkouts(workout: MutableList<Workout>) {
        binding.title.visibility = if (workout.isNotEmpty()) View.GONE else View.VISIBLE
        // Se necessario, qui potresti aggiornare lo stato dell'adapter
    }

    /* ---- Funzioni per il calcolo dinamico dell'altezza del pager ---- */
    private fun getItemTotalHeight(isExpanded: Boolean): Int {
        val baseCardHeight = if (isExpanded) 80.dpToPx(requireContext()) else 40.dpToPx(requireContext())
        val extraHeight = 16.dpToPx(requireContext())
        return baseCardHeight + extraHeight
    }

    private fun calculateVisibleItemCount(isExpanded: Boolean, onCalculated: (Int) -> Unit) {
        binding.root.post {
            val cardHeight = cardView.height
            val contaierHeight = binding.pagerContainer.height
            val bottomMargin = 30.dpToPx(requireContext())
            val availableHeight = cardHeight - bottomMargin
            val singleItemHeight = getItemTotalHeight(isExpanded)
            val visibleCount = (availableHeight / singleItemHeight) - 1
            onCalculated(if (visibleCount > 0) visibleCount else 1)
            Log.e("ssssss", cardHeight.toString())
            Log.e("ssssss", contaierHeight.toString())
            Log.e("ssssss", availableHeight.toString())
            Log.e("ssssss", singleItemHeight.toString())
            Log.e("ssssss", visibleCount.toString())
        }
    }

    private fun getHeight(finalHeight: Float) : Float {
        val metrics = requireContext().resources.displayMetrics
        val screenWidth  = metrics.widthPixels   // larghezza in px
        val screenHeight = metrics.heightPixels  // altezza in px

        val containerBottom = binding.pagerContainer.bottom
        val h = finalHeight - containerBottom
        return h
    }

    /**
     * Aggiorna l'altezza del pager in base allo spazio disponibile,
     * calcolando quanti item ci stanno.
     */
    private fun adjustPagerHeight(availableHeight: Float) {
        val isExpanded = isWeekMode
        val offset = 30.dpToPx(requireContext())
        val containerHeight = binding.relativeContainer.height
        val weekHeight = binding.exOneWeekCalendar.height
        val monthHeight = weekHeight * 6
        val calendarHeight = if (isExpanded) weekHeight else monthHeight
        val newHeight = containerHeight - calendarHeight - offset
        Log.e("sasso", newHeight.toString())

        val singleItemHeight = getItemTotalHeight(isExpanded)
        val visibleCount = (newHeight / singleItemHeight) - 1

        Log.e("sasso", singleItemHeight.toString())
        Log.e("sasso", visibleCount.toString())

        viewModel.setNItems(visibleCount)
    }

    private fun adjustPagerHeight() {
        val isExpanded = isWeekMode
        calculateVisibleItemCount(isExpanded) { visibleItemCount ->
            val newPagerHeight = visibleItemCount * getItemTotalHeight(isExpanded)
            viewPager.updateLayoutParams<LinearLayout.LayoutParams> {
                //height = newPagerHeight
            }

            Log.e("sssssssss", newPagerHeight.toString())

            val remaining = binding.pagerContainer.height - newPagerHeight
            Log.e("ssssssssssss", remaining.toString())
            // 2. Applica come paddingBottom (minimo 0)
            viewPager.setPadding(0, 0, 0, remaining)

            viewModel.setNItems(visibleItemCount)
        }
    }

    /* ---- Aggiornamento info nell'AppBar ---- */
    @SuppressLint("SetTextI18n")
    private fun updateTitle() {
        if (!isWeekMode) {
            monthCalendarView.findFirstVisibleMonth()?.yearMonth?.let { month ->
                binding.exOneYearText.text = month.year.toString()
                binding.exOneMonthText.text = month.month.displayText(short = false)
            }
        } else {
            weekCalendarView.findFirstVisibleWeek()?.let { week ->
                val firstDate = week.days.first().date
                val lastDate = week.days.last().date
                if (firstDate.yearMonth == lastDate.yearMonth) {
                    binding.exOneYearText.text = firstDate.year.toString()
                    binding.exOneMonthText.text = firstDate.month.displayText(short = false)
                } else {
                    binding.exOneMonthText.text = "${firstDate.month.displayText(short = false)} - ${lastDate.month.displayText(short = false)}"
                    binding.exOneYearText.text = if (firstDate.year == lastDate.year) firstDate.year.toString() else "${firstDate.year} - ${lastDate.year}"
                }
            }
        }
    }

    private fun Month.displayText(short: Boolean = true): String {
        val style = if (short) TextStyle.SHORT else TextStyle.FULL
        return getDisplayName(style, Locale.ENGLISH)
    }

    private fun DayOfWeek.displayText(uppercase: Boolean = false): String {
        val text = getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        return if (uppercase) text.uppercase(Locale.ENGLISH) else text
    }

    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDestroyView() {
        super.onDestroyView()
        binding.lineContainer.setOnTouchListener(null)
        binding.manageWorkoutsButton.setOnClickListener(null)
        binding.selectButton.setOnClickListener(null)
        binding.dayLayout.setOnClickListener(null)
        binding.left.setOnClickListener(null)
        binding.right.setOnClickListener(null)
        monthCalendarView.viewTreeObserver.removeOnGlobalLayoutListener {
            collapsedPosition = monthCalendarView.bottom.toFloat()
            if (isWeekMode) cardView.y = collapsedPosition
        }
        _binding = null
    }
}
