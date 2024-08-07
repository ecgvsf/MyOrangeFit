package com.example.myorangefit

import android.animation.AnimatorSet
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActionBar.LayoutParams
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.LayoutDirection
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myorangefit.databinding.ActivityMainBinding
import com.example.myorangefit.databinding.CalendarDayBinding
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var exerciseAdapter: ExerciseCalendarAdapter

    private lateinit var binding: ActivityMainBinding
    private val monthCalendarView: CalendarView get() = binding.exOneCalendar
    private val weekCalendarView: WeekCalendarView get() = binding.exOneWeekCalendar
    private lateinit var cardView: CardView

    private val selectedDates = mutableSetOf<LocalDate>()
    private var selectedDate: LocalDate? = null
    private var isWeekMode = false
    private val today = LocalDate.now()

    private lateinit var numDayTextView: TextView
    private lateinit var weekDayTextView: TextView

    private var expandedPosition = 0f
    private var collapsedPosition = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val daysOfWeek = daysOfWeek()

        databaseHelper = DatabaseHelperSingleton.getInstance(this)

        recyclerView = binding.recyclerViewExercise
        recyclerView.layoutManager = LinearLayoutManager(this)

        numDayTextView = findViewById(R.id.num_day)
        weekDayTextView = findViewById(R.id.week_day)

        cardView = findViewById(R.id.bottomSheet)
        val screenHeight = resources.displayMetrics.heightPixels
        cardView.updateLayoutParams {
            height = screenHeight
        }
        cardView.updateLayoutParams<RelativeLayout.LayoutParams> {
            bottomMargin = -screenHeight / 3
        }

        Log.d("dataa", "$screenHeight, ${cardView.layoutParams.height}")
        cardView.setOnTouchListener(cardViewTouchListener)

        binding.legendLayout.root.children
            .map { it as TextView }
            .forEachIndexed { index, textView ->
                textView.text = daysOfWeek[index].displayText()
                textView.setTextColor(Color.WHITE)
            }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        setupMonthCalendar(startMonth, endMonth, currentMonth, daysOfWeek)
        setupWeekCalendar(startMonth, endMonth, currentMonth, daysOfWeek)

        monthCalendarView.isInvisible = isWeekMode
        weekCalendarView.isInvisible = !isWeekMode

        updateTitle()
        updateDayInfo(today)

        cardView.post {
            expandedPosition = weekCalendarView.bottom.toFloat()
            collapsedPosition = monthCalendarView.bottom.toFloat()
            cardView.y = collapsedPosition
            adjustRecyclerViewHeight()
        }

        monthCalendarView.viewTreeObserver.addOnGlobalLayoutListener {
            collapsedPosition = monthCalendarView.bottom.toFloat()
            if (isWeekMode) {
                cardView.y = collapsedPosition
            }
        }

        binding.fab.setOnClickListener {
            val date = (selectedDate ?: today).toString()
            val intent = Intent(this, BodyPartActivity::class.java)
            intent.putExtra("selectedDate", date)
            intent.putExtra("calendar", 1)
            startActivity(intent)
        }

        binding.manageWorkoutsButton.setOnClickListener {
            val intent = Intent(this, ManageWorkoutActivity::class.java)
            startActivity(intent)
        }

        binding.left.setOnClickListener { scroll(-1) }
        binding.right.setOnClickListener { scroll(1) }
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
                    val newMode = if (deltaY < 0) true else false

                    // Verifica se la nuova modalità è diversa dall'attuale
                    if (newMode != isWeekMode) {
                        isWeekMode = newMode
                        val visibility: Int = if (isWeekMode){
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                        binding.selectButton.visibility = visibility
                        binding.editButton.visibility = visibility
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

        val weekHeight = weekCalendarView.height
        val visibleMonthHeight = weekHeight * monthCalendarView.findFirstVisibleMonth()?.weekDays.orEmpty().count()

        val oldHeight = if (isWeekMode) visibleMonthHeight else weekHeight
        val newHeight = if (isWeekMode) weekHeight else visibleMonthHeight

        val oldPosition = cardView.y
        val newPosition = if (isWeekMode) oldHeight else newHeight

        // Larghezza della linea
        val line = findViewById<View>(R.id.line)
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
                cardView.y = oldPosition + (newPosition - oldPosition) * (animatedHeight.toFloat() / newHeight)
            }
            doOnStart {
                if (!isWeekMode) {
                    weekCalendarView.isInvisible = true
                    monthCalendarView.isVisible = true
                }
                adjustRecyclerViewHeight()
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
                selectedDates.contains(date) -> {
                    dayContainer.setBackgroundResource(R.drawable.selected_bg)
                    textView.setTextColor(Color.WHITE) // Colore del testo selezionato
                }
                today == date -> {
                    dayContainer.setBackgroundResource(R.drawable.today_bg)
                    textView.setTextColor(Color.WHITE) // Colore del testo per il giorno corrente
                }
                else -> {
                    textView.setTextColor(Color.WHITE) // Colore del testo per i giorni normali
                    dayContainer.background = null
                }
            }

            val colorMap = mapOf(
                "1" to getColor(R.color.chestColor),
                "2" to getColor(R.color.backColor),
                "3" to getColor(R.color.legsColor),
                "4" to getColor(R.color.armsColor),
                "5" to getColor(R.color.shouldersColor),
                "6" to getColor(R.color.absColor)
            )

            val dateBodyPartMap = loadWorkouts()

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
        val previousDate = selectedDate
        selectedDate = date

        // Svuota la selezione esistente e aggiungi la nuova data
        selectedDates.clear()
        selectedDates.add(date)

        // Refresh both calendar views..
        if (previousDate != null) {
            monthCalendarView.notifyDateChanged(previousDate)
            weekCalendarView.notifyDateChanged(previousDate)
        }
        monthCalendarView.notifyDateChanged(date)
        weekCalendarView.notifyDateChanged(date)

        updateDayInfo(date)

        val workout = loadWorkoutsForDate(date)
        displayWorkouts(this, workout)
    }

    //---------------------------------------------------------------------------------------------------------------------------------------------
    /* ---------------------------------------------- Per Caricare gli Allenamenti nel Calendario ---------------------------------------------- */

    private fun loadWorkouts():  MutableMap<LocalDate, MutableSet<String>>{
        // Carica gli allenamenti dal database e aggiorna il calendario
        // Esegui il cerchietto colorato per i giorni con allenamenti
        val workoutCalendar = databaseHelper.getAllWorkoutCalendar()
        val dates = mutableSetOf<LocalDate>()
        val dateBodyPartMap = mutableMapOf<LocalDate, MutableSet<String>>()

        for (w in workoutCalendar) {
            val workout = databaseHelper.getWorkoutById(w.idWorkout)
            val date = LocalDate.parse(w.date)
            dates.add(date)
            if (!(dateBodyPartMap.containsKey(date))) {
                dateBodyPartMap[date] = mutableSetOf()
            }
            workout?.bodyPart?.let { dateBodyPartMap[date]!!.add(it) }
        }

        return dateBodyPartMap

        /*
        // Aggiungi decoratori solo se non esiste già un decoratore per quel giorno
        val existingDecorators = mutableSetOf<LocalDate>()
        dateBodyPartMap.forEach { (day, bodyParts) ->
            if (day !in existingDecorators) {
                val bodyPartColors = bodyParts.mapNotNull { colorMap[it] }
                ///calendarView.addDecorator(EventDecorator(setOf(day), bodyPartColors))
                existingDecorators.add(day)
            }
        }
         */
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------
    /* -------------------------------------------- Per Caricare gli Allenamenti nel Recycler View -------------------------------------------- */

    private fun loadWorkoutsForDate(date: LocalDate): MutableList<Workout> {
        val dateStr = date.toString()
        val workoutsId = databaseHelper.getWorkoutsIdForDate(dateStr)
        val workout = mutableListOf<Workout>()
        for (id in workoutsId) {
            databaseHelper.getWorkoutById(id)?.let {
                if (!workout.contains(it))
                    workout.add(it)
            }
        }

        return workout
    }

    private fun displayWorkouts(context: Context, workout: MutableList<Workout>) {
        val list = findViewById<RecyclerView>(R.id.recyclerViewExercise)
        if (workout.isNotEmpty()) {
            findViewById<TextView>(R.id.title).visibility = View.GONE
        } else {
            findViewById<TextView>(R.id.title).visibility = View.VISIBLE
        }
        exerciseAdapter = ExerciseCalendarAdapter(context, workout)
        list.adapter = exerciseAdapter
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    /* ---------------------------------------------- Per adattare l'altezza del RecyclerView ----------------------------------------------- */

    private fun adjustRecyclerViewHeight() {
        val screenHeight = resources.displayMetrics.heightPixels
        val cardViewH = cardView.height
        val availableHeight = screenHeight - cardViewH

        // DA RIFARE
        Log.d("dataaa", "1 $isWeekMode, $cardViewH $screenHeight $availableHeight, ${recyclerView.height}")
        if (!isWeekMode)
            recyclerView.updateLayoutParams<LinearLayout.LayoutParams> {
                height = LayoutParams.WRAP_CONTENT
            }
        else
            recyclerView.updateLayoutParams<LinearLayout.LayoutParams> {
                height = LayoutParams.WRAP_CONTENT
            }

        Log.d("dataaa", "2 $isWeekMode, $cardViewH $screenHeight $availableHeight, ${recyclerView.height}")
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

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
    }
}