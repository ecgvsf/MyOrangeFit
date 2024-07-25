package com.example.myorangefit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var exerciseAdapter: ExerciseCalendarAdapter
    private var isMonthlyView = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityManager.add(this)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewExercise)
        recyclerView.layoutManager = LinearLayoutManager(this)

        databaseHelper = DatabaseHelperSingleton.getInstance(this)
        calendarView = findViewById(R.id.calendarView)
        val today = CalendarDay.from(LocalDate.now())
        calendarView.selectedDate = today
        calendarView.currentDate = today
        val num = findViewById<TextView>(R.id.num_day)
        val week = findViewById<TextView>(R.id.week_day)
        num.text = today.day.toString()
        week.text = LocalDate.now().dayOfWeek.toString().substring(0, 3)

        // Configura il calendario
        calendarView.state().edit()
            .setFirstDayOfWeek(DayOfWeek.MONDAY)
            .setCalendarDisplayMode(CalendarMode.MONTHS)
            .commit()

        calendarView.setOnDateChangedListener { _, date, _ ->
            // Gestione della selezione del giorno
            val workout = loadWorkoutsForDate(date)
            Log.d("morona", "$workout")
            displayWorkouts(this, workout)
        }

        // Imposta il listener per il cambio del mese
        calendarView.setOnMonthChangedListener { widget, date ->
            onMonthChanged(date)
        }

        val fab: ImageView = findViewById(R.id.fab)
        fab.setOnClickListener {
            var date = getCurrentDate()
            val selectedDate = calendarView.selectedDate
            val selectedString = selectedDate?.let { it1 -> formatCalendarDay(it1) }
            Log.d("dataselezionata", selectedString.toString())
            if (selectedString != null) {
                date = selectedString.toString()
            }
            val intent = Intent(this, BodyPartActivity::class.java)
            intent.putExtra("selectedDate", date)
            intent.putExtra("calendar", 1)
            startActivity(intent)
        }

        val manageWorkoutsButton: ImageView = findViewById(R.id.manageWorkoutsButton)
        manageWorkoutsButton.setOnClickListener {
            val intent = Intent(this, ManageWorkoutActivity::class.java)
            startActivity(intent)
        }

        val line: LinearLayout = findViewById(R.id.lineContainer)
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                // Gestione dello scroll
                if (abs(distanceY) > abs(distanceX)) {
                    if (distanceY > 0) {
                        // Scroll verso il basso
                        if (!isMonthlyView) switchToMonthlyView()
                    } else {
                        // Scroll verso l'alto
                        if (isMonthlyView) switchToWeeklyView()
                    }
                    return true
                }
                return false
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                Log.d("SCORRIMENTO", "porcodio")
                e1 ?: return false
                e2 ?: return false
                val diffY = e2.y - e1.y
                if (abs(diffY) > abs(velocityX)) {
                    if (diffY > 0) {
                        // Swipe verso il basso
                        if (!isMonthlyView) switchToMonthlyView()
                    } else {
                        // Swipe verso l'alto
                        if (isMonthlyView) switchToWeeklyView()
                    }
                    return true
                }
                return false
            }
        })

        line.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }

        line.setOnClickListener {
            if (isMonthlyView) {
                switchToWeeklyView()
            } else {
                switchToMonthlyView()
            }
        }

        // Carica gli allenamenti e aggiorna il calendario
        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val currentDate = LocalDate.parse(getCurrentDate(), format)

        loadWorkouts(CalendarDay.from(currentDate))
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

    private fun loadWorkoutsForDate(date: CalendarDay): MutableList<Workout> {
        val num = findViewById<TextView>(R.id.num_day)
        val week = findViewById<TextView>(R.id.week_day)

        num.text = date.day.toString()
        val localDate = LocalDate.of(date.year, date.month, date.day)
        week.text = localDate.dayOfWeek.toString().substring(0, 3)

        val dateStr = formatCalendarDay(date)
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

    private fun loadWorkouts(currentDate: CalendarDay) {
        // Carica gli allenamenti dal database e aggiorna il calendario
        // Esegui il cerchietto colorato per i giorni con allenamenti
        val workoutCalendar = databaseHelper.getAllWorkoutCalendar()
        val dates = mutableSetOf<CalendarDay>()
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dateBodyPartMap = mutableMapOf<CalendarDay, MutableSet<String>>()

        for (w in workoutCalendar) {
            val workout = databaseHelper.getWorkoutById(w.idWorkout)
            val date = LocalDate.parse(w.date, dateFormat)
            val calendarDay = CalendarDay.from(date)
            dates.add(calendarDay)
            if (!(dateBodyPartMap.containsKey(calendarDay))) {
                dateBodyPartMap[calendarDay] = mutableSetOf()
            }
            workout?.bodyPart?.let { dateBodyPartMap[calendarDay]!!.add(it) }
        }

        val colorMap = mapOf(
            "1" to getColor(R.color.chestColor),
            "2" to getColor(R.color.backColor),
            "3" to getColor(R.color.legsColor),
            "4" to getColor(R.color.armsColor),
            "5" to getColor(R.color.shouldersColor),
            "6" to getColor(R.color.absColor)
        )

        // Aggiungi decoratori solo se non esiste già un decoratore per quel giorno
        val existingDecorators = mutableSetOf<CalendarDay>()
        dateBodyPartMap.forEach { (day, bodyParts) ->
            if (day !in existingDecorators) {
                val bodyPartColors = bodyParts.mapNotNull { colorMap[it] }
                calendarView.addDecorator(EventDecorator(setOf(day), bodyPartColors))

                existingDecorators.add(day)
            }
        }
    }

    private fun onMonthChanged(date: CalendarDay) {
        // Questo metodo viene chiamato ogni volta che il mese cambia
        Log.d("MainActivity", "Mese cambiato: ${date.year}-${date.month}")
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun formatCalendarDay(calendarDay: CalendarDay, pattern: String = "yyyy-MM-dd"): String {
        // Estrai l'anno, il mese e il giorno dal CalendarDay
        val year = calendarDay.year
        val month = calendarDay.month
        val day = calendarDay.day

        // Crea un oggetto LocalDate con i dati estratti
        val localDate = LocalDate.of(year, month, day)

        // Crea un DateTimeFormatter con il pattern specificato
        val formatter = DateTimeFormatter.ofPattern(pattern)

        // Converte LocalDate in una stringa formattata
        return localDate.format(formatter)
    }

    private fun switchToWeeklyView() {
        Log.d("SCORRIMENTO", "Passa alla vista settimanale")

        isMonthlyView = false
        val editbt = findViewById<ImageView>(R.id.editButton)
        val selectbt = findViewById<ImageView>(R.id.selectButton)
        editbt.visibility = View.VISIBLE
        selectbt.visibility = View.VISIBLE
        val line = findViewById<View>(R.id.line)
        val layoutParams = line.layoutParams
        layoutParams.width += 64
        line.layoutParams = layoutParams
        calendarView.state().edit()
            .setCalendarDisplayMode(CalendarMode.WEEKS)
            .commit()
        TransitionManager.beginDelayedTransition(findViewById(R.id.root), AutoTransition())
        // Logica per aggiornare UI in modalità settimanale
        updateUIForWeeklyView()
    }

    private fun switchToMonthlyView() {
        Log.d("SCORRIMENTO", "Passa alla vista mensile")

        isMonthlyView = true
        val editbt = findViewById<ImageView>(R.id.editButton)
        val selectbt = findViewById<ImageView>(R.id.selectButton)
        editbt.visibility = View.GONE
        selectbt.visibility = View.GONE
        val line = findViewById<View>(R.id.line)
        val layoutParams = line.layoutParams
        layoutParams.width -= 64
        line.layoutParams = layoutParams
        calendarView.state().edit()
            .setCalendarDisplayMode(CalendarMode.MONTHS)
            .commit()
        TransitionManager.beginDelayedTransition(findViewById(R.id.root), AutoTransition())
        // Logica per aggiornare UI in modalità mensile
        updateUIForMonthlyView()
    }

    private fun updateUIForWeeklyView() {
        // Modifica le informazioni visualizzate nella card view per la modalità settimanale
        val title = findViewById<TextView>(R.id.title)
        // Aggiungi logica per mostrare peso, serie e ripetizioni
    }

    private fun updateUIForMonthlyView() {
        // Modifica le informazioni visualizzate nella card view per la modalità mensile
        val title = findViewById<TextView>(R.id.title)
        // Aggiungi logica per rimuovere peso, serie e ripetizioni
    }


    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
    }
}
