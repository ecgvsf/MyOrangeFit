package com.example.myorangefit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.prolificinteractive.materialcalendarview.CalendarDay
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityManager.add(this)
        setContentView(R.layout.activity_main)

        databaseHelper = DatabaseHelperSingleton.getInstance(this)
        calendarView = findViewById(R.id.calendarView)

        // Configura il calendario
        calendarView.setOnDateChangedListener { widget, date, selected ->
            // Gestione della selezione del giorno
        }

        // Imposta il listener per il cambio del mese
        calendarView.setOnMonthChangedListener { widget, date ->
            onMonthChanged(date)
        }

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            var date = getCurrentDate()
            val selectedDate = calendarView.selectedDate
            val selectedString = selectedDate?.let { it1 -> formatCalendarDay(it1) }
            Log.d("dataselezionata",selectedString.toString())
            if (selectedString != null) {
                date = selectedString.toString()
            }
            val intent = Intent(this, BodyPartActivity::class.java)
            intent.putExtra("selectedDate", date)
            intent.putExtra("calendar", 1)
            startActivity(intent)
        }

        val manageWorkoutsButton: Button = findViewById(R.id.manageWorkoutsButton)
        manageWorkoutsButton.setOnClickListener {
            val intent = Intent(this, ManageWorkoutActivity::class.java)
            startActivity(intent)
        }

        // Carica gli allenamenti e aggiorna il calendario
        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val currentDate = LocalDate.parse(getCurrentDate(),format)

        loadWorkouts(CalendarDay.from(currentDate))

    }

    private fun loadWorkouts(currentDate: CalendarDay) {
        // Carica gli allenamenti dal database e aggiorna il calendario
        // Esegui il cerchietto colorato per i giorni con allenamenti
        /*val workoutCalendar = databaseHelper.getWorkoutsCalendarByMonths(
            currentDate.year.toString(),
            currentDate.month.toString()
        )*/
        val workoutCalendar = databaseHelper.getAllWorkoutCalendar()
        val dates = mutableSetOf<CalendarDay>()
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dateBodyPartMap = mutableMapOf<CalendarDay, MutableSet<String>>()

        Log.d("ciopp", "${currentDate.year} ${currentDate.month}")

        for (w in workoutCalendar) {
            val workout = databaseHelper.getWorkoutById(w.idWorkout)
            val date = LocalDate.parse(w.date, dateFormat)
            val calendarDay = CalendarDay.from(date)
            dates.add(calendarDay)
            Log.d("ciopp", "$calendarDay")
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
            // Add more body parts and colors if needed
        )

        Log.d("sticazzi", "$dateBodyPartMap")

        // Aggiungi decoratori solo se non esiste già un decoratore per quel giorno
        val existingDecorators = mutableSetOf<CalendarDay>()
        dateBodyPartMap.forEach { (day, bodyParts) ->
            if (day !in existingDecorators) {
                val bodyPartColors = bodyParts.mapNotNull { colorMap[it] }
                calendarView.addDecorator(EventDecorator(setOf(day), bodyPartColors))
                existingDecorators.add(day)
                Log.d("sticazzi", "$day $bodyPartColors")
            }

        }
    }

    private fun onMonthChanged(date: CalendarDay) {
        // Questo metodo viene chiamato ogni volta che il mese cambia
        Log.d("MainActivity", "Mese cambiato: ${date.year}-${date.month}")
        // Qui puoi eseguire il codice necessario quando il mese cambia
        //loadWorkouts(date)
    }


    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
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
}
