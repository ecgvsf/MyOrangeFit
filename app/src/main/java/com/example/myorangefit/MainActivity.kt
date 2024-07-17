package com.example.myorangefit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            var date = getCurrentDate()
            val selectedDate = calendarView.selectedDate
            Log.d("data",selectedDate.toString())
            if (selectedDate != null) {
                date = selectedDate.toString()
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
        loadWorkouts()

    }

    private fun loadWorkouts() {
        // Carica gli allenamenti dal database e aggiorna il calendario
        // Esegui il cerchietto colorato per i giorni con allenamenti
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
    }
}
