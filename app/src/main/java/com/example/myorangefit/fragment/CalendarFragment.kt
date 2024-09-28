package com.example.myorangefit.fragment

import android.animation.AnimatorSet
import android.os.Bundle

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActionBar.LayoutParams
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.children
import androidx.core.view.isEmpty
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.myorangefit.R
import com.example.myorangefit.activity.MainActivity
import com.example.myorangefit.activity.ManageWorkoutActivity
import com.example.myorangefit.adapter.ExerciseCalendarAdapter
import com.example.myorangefit.adapter.ExerciseFragment
import com.example.myorangefit.adapter.ExercisePagerAdapter
import com.example.myorangefit.adapter.FadeItemAnimator
import com.example.myorangefit.async.WorkoutViewModel
import com.example.myorangefit.async.WorkoutViewModelFactory
import com.example.myorangefit.database.DatabaseHelper
import com.example.myorangefit.database.DatabaseHelperSingleton
import com.example.myorangefit.databinding.CalendarDayBinding
import com.example.myorangefit.databinding.FragmentCalendarBinding
import com.example.myorangefit.model.Workout
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class CalendarFragment : Fragment() {

    private lateinit var today: LocalDate

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var viewModel: WorkoutViewModel

    private lateinit var dateBodyPartMap: MutableMap<LocalDate, MutableSet<String>>

    //private lateinit var recyclerView: RecyclerView
    //private lateinit var exerciseAdapter: ExerciseCalendarAdapter

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
        today = (arguments?.getSerializable("today") as? LocalDate)!!


        contx = requireContext()
        databaseHelper = DatabaseHelperSingleton.getInstance(contx)
        viewModel = ViewModelProvider(requireActivity())[WorkoutViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)

        monthCalendarView = binding.exOneCalendar
        weekCalendarView = binding.exOneWeekCalendar


        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = binding.pager

        calculateItemsPerPage { itemsPerPage ->
            viewModel.setNItems(itemsPerPage)
        }

        viewModel.nItems.observe(viewLifecycleOwner) { num ->
            nItems = num
            Log.e("sss", "$nItems, $num")
            nItems = 4
        }

        viewModel.allWorkouts.observe(viewLifecycleOwner) { workoutMap ->
            dateBodyPartMap = workoutMap
        }

        viewModel.selectedData.observe(viewLifecycleOwner) { selected ->
            selectedDate = selected
            viewModel.loadWorkoutsForDate(selected)
        }

        // Osserva gli allenamenti caricati e chiamare displayWorkouts per visualizzarli
        viewModel.workoutsForDate.observe(viewLifecycleOwner) { workoutsForDate ->
            val selectedDate = viewModel.selectedData.value
            val workouts = workoutsForDate[selectedDate] ?: emptyList()

            displayWorkouts(workouts.toMutableList())
            updateTitle()
            if (selectedDate != null) {
                updateDayInfo(selectedDate)
            }

            // Crea l'adapter per il ViewPager2
            val exerciseData =
                workouts.chunked(nItems).map { it.toMutableList() }.toMutableList()
            val d = selectedDate ?: today

            val exercisePagerAdapter =
                ExercisePagerAdapter(requireActivity(), exerciseData, d, isWeekMode)
            viewPager.adapter = exercisePagerAdapter
            // Imposta il limite delle pagine fuori schermo
            if (workouts.isNotEmpty())
                viewPager.setOffscreenPageLimit(exerciseData.size)
        }

        val daysOfWeek = daysOfWeek()

        numDayTextView = binding.numDay
        weekDayTextView = binding.weekDay

        cardView = binding.bottomSheet
        val screenHeight = resources.displayMetrics.heightPixels
        cardView.updateLayoutParams {
            height = screenHeight
        }
        cardView.updateLayoutParams<RelativeLayout.LayoutParams> {
            bottomMargin = -90
        }

        binding.lineContainer.setOnTouchListener(cardViewTouchListener)
        //monthCalendarView.setOnTouchListener(cardViewTouchListener)
        //weekCalendarView.setOnTouchListener(cardViewTouchListener)

        binding.legendLayout.root.children
            .map { it as TextView }
            .forEachIndexed { index, textView ->
                textView.text = daysOfWeek[index].displayText()
                textView.setTextColor(Color.WHITE)
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
            expandedPosition = weekCalendarView.bottom.toFloat()
            collapsedPosition = monthCalendarView.bottom.toFloat() + 32f
            cardView.y = collapsedPosition
            adjustPagerHeight()
        }

        Log.d("dataaaa", "$screenHeight, ${cardView.layoutParams.height}")

        monthCalendarView.viewTreeObserver.addOnGlobalLayoutListener {
            collapsedPosition = monthCalendarView.bottom.toFloat()
            if (isWeekMode) {
                cardView.y = collapsedPosition
            }
        }

        Log.d("dataaaaa", "$screenHeight, ${cardView.layoutParams.height}")

        binding.manageWorkoutsButton.setOnClickListener {
            val intent = Intent(contx, ManageWorkoutActivity::class.java)
            startActivity(intent)
        }

        binding.selectButton.setOnClickListener {
            val map = viewModel.workoutsForDate.value
            map?.let {
                val workouts = map[selectedDate] ?: emptyList()
                if (workouts.isNotEmpty())
                    selectionMode()
            }
        }

        binding.dayLayout.setOnClickListener { scrollToday() }
        binding.left.setOnClickListener { scroll(-1) }
        binding.right.setOnClickListener { scroll(1) }


        binding.trashButton.setOnClickListener {
            val adapter = viewPager.adapter as? ExercisePagerAdapter
            adapter?.let {
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
    }

    //---------------------------------------------------------------------------------------------------------------------------------------------
    /* ------------------------------------------------------- Per Scorrere il calendario ------------------------------------------------------ */

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

    //-------------------------------------------------------------------------------------------------------------------------------------------
    /* ----------------------------------------------- Per Scorrere il calendario con le Frecce ---------------------------------------------- */

    private fun scroll(direction: Long) {
        if (!isWeekMode) { //calendario mensile
            val dateToShow = monthCalendarView.findFirstVisibleMonth()?.yearMonth?.plusMonths(direction)
            if (dateToShow != null) {
                monthCalendarView.scrollToMonth(dateToShow)
            }
        } else { //calendario settimanale
            val dateToShow = weekCalendarView.findFirstVisibleWeek()?.days?.first()?.date?.plusWeeks(direction)
            if (dateToShow != null) {
                weekCalendarView.scrollToWeek(dateToShow)
            }
        }
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    /* -------------------------------------------- Per Aggiornare le Info del Giorno Selezionato -------------------------------------------- */

    @SuppressLint("SetTextI18n")
    private fun updateDayInfo(date: LocalDate) {
        numDayTextView.text = date.dayOfMonth.toString()
        weekDayTextView.text = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).substring(0, 3).lowercase()
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    /* ------------------------------------------- Per Animare il Calendario e la Card con le Info ------------------------------------------- */

    private val cardViewTouchListener = object : View.OnTouchListener {
        private var initialTouchY: Float = 0f

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {


            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchY = event.rawY
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    // Non fare nulla durante il movimento
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    val deltaY = event.rawY - initialTouchY
                    // Determina se l'utente vuole passare alla modalità mensile o settimanale
                    val newMode = if (deltaY < 0) {
                        true
                    }
                    else if (deltaY > 0) {
                        val adapter = viewPager.adapter as? ExercisePagerAdapter

                        // Verifica se l'adapter non è null
                        adapter?.let {
                            if (isSelectionMode)
                                selectionMode()
                        }
                        false
                    }
                    else
                        return false

                    // Verifica se la nuova modalità è diversa dall'attuale
                    if (newMode != isWeekMode) {
                        isWeekMode = newMode
                        val visibility: Int = if (isWeekMode){
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                        binding.selectButton.visibility = visibility
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
        val dateToShow = selectedDate ?: today
        if (!isWeekMode) {
            monthCalendarView.scrollToMonth(dateToShow.yearMonth)
        } else {
            weekCalendarView.scrollToWeek(dateToShow.startOfWeek())
        }

        var weekHeight = weekCalendarView.height
        val visibleMonthHeight = weekHeight * monthCalendarView.findFirstVisibleMonth()?.weekDays.orEmpty().count()
        weekHeight += 32

        val oldHeight = if (isWeekMode) visibleMonthHeight else weekHeight
        val newHeight = if (isWeekMode) weekHeight else visibleMonthHeight

        val oldPosition = cardView.y
        val newPosition = if (isWeekMode) oldHeight + 32 else newHeight + 32

        // Larghezza della linea
        val line = binding.line
        val oldLineWidth = line.layoutParams.width
        val newLineWidth = if (isWeekMode) oldLineWidth + 64 else oldLineWidth - 64

        // Animatore per altezza del calendario e posizione della CardView
        val heightAnimator = ValueAnimator.ofInt(oldHeight, newHeight).apply {
            addUpdateListener { anim ->
                val animatedHeight = anim.animatedValue as Int
                monthCalendarView.updateLayoutParams {
                    height = animatedHeight
                }
                monthCalendarView.children.forEach { child ->
                    child.requestLayout()
                }
                // Aggiorna la posizione della CardView in base all'altezza del calendario
                cardView.y = (oldPosition + (newPosition - oldPosition) * (animatedHeight.toFloat() / newHeight))
            }
            doOnStart {
                if (!isWeekMode) {
                    weekCalendarView.isInvisible = true
                    monthCalendarView.isVisible = true
                }
                val adapter = viewPager.adapter as ExercisePagerAdapter
                for (i in 0..adapter.itemCount) {
                    adapter.toggleExpansion(i,isWeekMode)
                }
                adjustPagerHeight()
                //adjustRecyclerViewItem()
            }
            doOnEnd {
                if (isWeekMode) {
                    weekCalendarView.isVisible = true
                    monthCalendarView.isInvisible = true
                } else {
                    monthCalendarView.updateLayoutParams { height = WRAP_CONTENT }
                }
                updateTitle()
            }
            duration = 300
        }

        // Animatore per la larghezza della linea
        val lineWidthAnimator = ValueAnimator.ofInt(oldLineWidth, newLineWidth).apply {
            addUpdateListener { anim ->
                val animatedWidth = anim.animatedValue as Int
                line.layoutParams.width = animatedWidth
                line.requestLayout()
            }
            duration = 300
        }

        // AnimatorSet per combinare le due animazioni
        val animatorSet = AnimatorSet().apply {
            playTogether(heightAnimator, lineWidthAnimator)
        }

        animatorSet.start()
        Log.d("dataaaaa", "${cardView.height}")
    }

    /*
    private fun adjustRecyclerViewItem() {
        // Ottieni l'adapter dal RecyclerView
        val adapter = recyclerView.adapter as? ExerciseCalendarAdapter

        // Verifica se l'adapter non è null
        adapter?.let {
            // Itera sugli item visibili del RecyclerView
            for (i in 0 until recyclerView.childCount) {
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as? ExerciseCalendarAdapter.ExerciseCalendarViewHolder
                viewHolder?.let { holder ->
                    // Chiama toggleExpansion per ciascun item visibile
                    adapter.toggleExpansion(holder, isWeekMode)
                } ?: run {
                    adapter.notifyItemChanged(i)
                }
            }
        }
    }
    */

    private fun selectionMode() {
        // Ottieni l'adapter dal RecyclerView
        val adapter = viewPager.adapter as? ExercisePagerAdapter

        // Verifica se l'adapter non è null
        adapter?.let {
            if (isSelectionMode) {
                adapter.setSwipe(true)
                binding.trashButton.visibility = View.GONE
                //binding.editButton.visibility = View.GONE
            } else {
                adapter.setSwipe(false)
                binding.trashButton.visibility = View.VISIBLE
                //binding.editButton.visibility = View.VISIBLE
            }
            for (i in 0..adapter.itemCount)
                adapter.toggleSelection(i, isSelectionMode)

            isSelectionMode = !isSelectionMode
        }
    }

    private fun LocalDate.startOfWeek(): LocalDate {
        return this.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    /* ---------------------------------------------------- Per Inizializzare i Calendari ---------------------------------------------------- */

    private fun setupMonthCalendar(
        startMonth: YearMonth,
        endMonth: YearMonth,
        currentMonth: YearMonth,
        daysOfWeek: List<DayOfWeek>,
    ) {
        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay
            val textView = CalendarDayBinding.bind(view).exOneDayText
            val dotContainer = CalendarDayBinding.bind(view).dotContainer
            val dayContainer = CalendarDayBinding.bind(view).dayContainer

            init {
                view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate) {
                        dateClicked(date = day.date)
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

        monthCalendarView.monthScrollListener = {
            updateTitle()
        }

        monthCalendarView.setup(startMonth, endMonth, daysOfWeek.first())
        monthCalendarView.scrollToMonth(currentMonth)
    }

    private fun setupWeekCalendar(
        startMonth: YearMonth,
        endMonth: YearMonth,
        currentMonth: YearMonth,
        daysOfWeek: List<DayOfWeek>,
    ) {
        class WeekDayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: WeekDay
            val textView = CalendarDayBinding.bind(view).exOneDayText
            val dotContainer = CalendarDayBinding.bind(view).dotContainer
            val dayContainer = CalendarDayBinding.bind(view).dayContainer

            init {
                view.setOnClickListener {
                    if (day.position == WeekDayPosition.RangeDate) {
                        dateClicked(date = day.date)
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

        weekCalendarView.weekScrollListener = {
            updateTitle()
        }

        weekCalendarView.setup(
            startMonth.atStartOfMonth(),
            endMonth.atEndOfMonth(),
            daysOfWeek.first(),
        )
        weekCalendarView.scrollToWeek(currentMonth.atStartOfMonth())
    }

    private fun bindDate(date: LocalDate, dayContainer: LinearLayout, dotContainer: LinearLayout, textView: TextView, isSelectable: Boolean) {
        // Pulisci i vecchi pallini
        dotContainer.removeAllViews()

        textView.text = date.dayOfMonth.toString()
        if (isSelectable) {
            when {
                selectedDate == date -> {
                    dayContainer.setBackgroundResource(R.drawable.selected_bg)
                    textView.setTextColor(Color.WHITE) // Colore del testo selezionato
                }
                today == date -> {
                    dayContainer.setBackgroundResource(R.drawable.today_bg)
                    textView.setTextColor(Color.WHITE) // Colore del testo per il giorno corrente
                }
                else -> {
                    textView.setTextColor(Color.WHITE) // Colore del testo per i giorni normali
                    dayContainer.setBackgroundResource(R.drawable.day_bg)
                }
            }

            val colorMap = mapOf(
                "1" to getColor(contx, R.color.chestColor),
                "2" to getColor(contx, R.color.backColor),
                "3" to getColor(contx, R.color.legsColor),
                "4" to getColor(contx, R.color.armsColor),
                "5" to getColor(contx, R.color.shouldersColor),
                "6" to getColor(contx, R.color.absColor)
            )

            // Aggiungi i pallini per le parti del corpo allenate in questo giorno
            val bodyParts = dateBodyPartMap[date] ?: emptySet()
            bodyParts.forEach { bodyPart ->
                val color = colorMap[bodyPart]
                if (color != null) {
                    val dotView = View(dotContainer.context).apply {
                        layoutParams = LinearLayout.LayoutParams(16, 16).apply {
                            marginEnd = 4 // Aggiungi uno spazio tra i pallini
                        }
                        background = ContextCompat.getDrawable(context, R.drawable.circle) // Un drawable per i pallini rotondi
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

        {
            TODO(
                "da correggere l'aggiornamento della grafica del giorno " +
                        "selezionato che a volte si bugga quando cambi giorno rapidamente"
            )
        }

        isSelectionMode = false

        binding.trashButton.visibility = View.GONE
        //binding.editButton.visibility = View.GONE

        val previousDate = selectedDate

        viewModel.updateData(date)

        // Refresh both calendar views..
        previousDate?.let {
            monthCalendarView.notifyDateChanged(it)
            weekCalendarView.notifyDateChanged(it)
            Log.e("sss", "changing $previousDate, $date")
        }
        monthCalendarView.notifyDateChanged(date)
        weekCalendarView.notifyDateChanged(date)
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------
    /* -------------------------------------------- Per Caricare gli Allenamenti nel Recycler View -------------------------------------------- */

    private fun displayWorkouts(workout: MutableList<Workout>) {
        if (workout.isNotEmpty()) {
            binding.title.visibility = View.GONE
        } else {
            binding.title.visibility = View.VISIBLE
        }

        // Imposta lo stato espanso o contratto nell'adapter in base alla modalità corrente
        //exerciseAdapter.setExpandedMode(isWeekMode)
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    /* ---------------------------------------------- Per adattare l'altezza del RecyclerView ----------------------------------------------- */


    private fun adjustPagerHeight() {
        if (!isWeekMode)
            viewPager.updateLayoutParams<LinearLayout.LayoutParams> {
                height = (resources.getDimension(R.dimen.month_item_height).toInt() + 16.dpToPx(contx)) * nItems
            }
        else
            viewPager.updateLayoutParams<LinearLayout.LayoutParams> {
                height = LayoutParams.MATCH_PARENT
            }
    }

    private fun calculateItemsPerPage(onCalculated: (Int) -> Unit) {
        // Usa post per ottenere l'altezza della root view dopo che è stata disegnata
        binding.root.post {
            val frameHeight = binding.root.height
            val weekCalendarHeight = weekCalendarView.height

            val wLocation = IntArray(2)
            weekCalendarView.getLocationOnScreen(wLocation)

            val hWeek = wLocation[1] + weekCalendarHeight

            // Calcola l'altezza disponibile per il RecyclerView
            val cardHeightWeek = frameHeight - hWeek
            val recyclerViewWeekHeight = cardHeightWeek - 40.dpToPx(contx) - 20.dpToPx(contx)

            // Calcola il numero di item visibili nel RecyclerView
            val itemWeek = recyclerViewWeekHeight / 85.dpToPx(contx)

            Log.e("sss", "w: $itemWeek, $frameHeight")

            // Usa il callback per restituire il valore
            onCalculated(itemWeek)
        }
    }


    //-------------------------------------------------------------------------------------------------------------------------------------------
    /* ------------------------------------------------- Per Aggiornare le Info nella AppBar ------------------------------------------------- */

    @SuppressLint("SetTextI18n")
    private fun updateTitle() {
        if (!isWeekMode) {
            val month = monthCalendarView.findFirstVisibleMonth()?.yearMonth ?: return
            binding.exOneYearText.text = month.year.toString()
            binding.exOneMonthText.text = month.month.displayText(short = false)
        } else {
            val week = weekCalendarView.findFirstVisibleWeek() ?: return
            // In week mode, we show the header a bit differently because
            // an index can contain dates from different months/years.
            val firstDate = week.days.first().date
            val lastDate = week.days.last().date
            if (firstDate.yearMonth == lastDate.yearMonth) {
                binding.exOneYearText.text = firstDate.year.toString()
                binding.exOneMonthText.text = firstDate.month.displayText(short = false)
            } else {
                binding.exOneMonthText.text =
                    firstDate.month.displayText(short = false) + " - " +
                            lastDate.month.displayText(short = false)
                if (firstDate.year == lastDate.year) {
                    binding.exOneYearText.text = firstDate.year.toString()
                } else {
                    binding.exOneYearText.text = "${firstDate.year} - ${lastDate.year}"
                }
            }
        }
    }

    private fun Month.displayText(short: Boolean = true): String {
        val style = if (short) TextStyle.SHORT else TextStyle.FULL
        return getDisplayName(style, Locale.ENGLISH)
    }

    private fun DayOfWeek.displayText(uppercase: Boolean = false): String {
        return getDisplayName(TextStyle.SHORT, Locale.ENGLISH).let { value ->
            if (uppercase) value.uppercase(Locale.ENGLISH) else value
        }
    }

    fun Int.dpToPx(context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (this * density).toInt()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onDestroyView() {
        super.onDestroyView()
        binding.lineContainer.setOnTouchListener(null)
        //monthCalendarView.setOnTouchListener(null)
        //weekCalendarView.setOnTouchListener(null)
        binding.manageWorkoutsButton.setOnClickListener (null)
        binding.selectButton.setOnClickListener(null)
        binding.dayLayout.setOnClickListener (null)
        binding.left.setOnClickListener (null)
        binding.right.setOnClickListener (null)
        monthCalendarView.viewTreeObserver.removeOnGlobalLayoutListener {
            collapsedPosition = monthCalendarView.bottom.toFloat()
            if (isWeekMode) {
                cardView.y = collapsedPosition
            }
        }
        _binding = null

    }
}