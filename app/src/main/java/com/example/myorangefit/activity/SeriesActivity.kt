package com.example.myorangefit.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myorangefit.R
import com.example.myorangefit.adapter.SeriesAdapter
import com.example.myorangefit.compose.Clock
import com.example.myorangefit.compose.Scale
import com.example.myorangefit.database.DatabaseHelper


class SeriesActivity : AppCompatActivity() {

    private lateinit var weightActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var timeActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var seriesAdapter: SeriesAdapter
    private val seriesList = mutableListOf<Pair<Float, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityManager.add(this)
        setContentView(R.layout.activity_series)

        databaseHelper = DatabaseHelper(this)

        val data = intent.getStringExtra("date").orEmpty()
        val id = intent.getIntExtra("id_workout", -1)
        val workout = databaseHelper.getWorkoutById(id)

        val title = findViewById<TextView>(R.id.exercise_title)
        title.text = workout?.name

        // Inizializza la RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.series_container)
        recyclerView.layoutManager = LinearLayoutManager(this)
        seriesAdapter = SeriesAdapter(seriesList)
        recyclerView.adapter = seriesAdapter

        weightActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val weight = data.getFloatExtra("weight", -1f)
                    val reps = data.getIntExtra("reps", -1)
                    // Aggiorna la UI con il risultato della Scale Activity
                    seriesList.add(Pair(weight, reps))
                    seriesAdapter.notifyItemInserted(seriesList.size-1)
                }
            }
        }

        timeActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val time = data.getIntExtra("time", -1)
                    // Aggiorna la UI con il risultato dell'Another Activity
                    seriesList.add(Pair(-7f, time))
                    seriesAdapter.notifyItemInserted(seriesList.size-1)
                }
            }
        }

        val button = findViewById<Button>(R.id.add_series_button)
        button.setOnClickListener { v: View? ->
            when (workout?.type) {
                1 -> {
                    val intent = Intent(this@SeriesActivity, Scale::class.java)
                    weightActivityResultLauncher.launch(intent)
                }
                0 -> {
                    val intent = Intent(this@SeriesActivity, Clock::class.java)
                    timeActivityResultLauncher.launch(intent)
                }
            }

        }

        val cancelButton = findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            ActivityManager.finishAll()
            startActivity(Intent(this, MainActivity::class.java))
        }

        val saveButton = findViewById<Button>(R.id.save_button)
        saveButton.setOnClickListener {
            if (seriesList.isNotEmpty()) {
                val notes = findViewById<EditText>(R.id.general_notes).text.toString()
                databaseHelper.insertWorkoutInCalendar(id, data, notes)
                var position = 0
                for (serie: Pair<Float, Int> in seriesList) {
                    position++
                    val reps = if (serie.first == -7f) (null) else (serie.second)
                    val weight = if (serie.first == -7f) (null) else (serie.first)
                    val time = if (serie.first != -7f) (null) else (serie.second)
                    databaseHelper.insertSeries(id, data, position, reps, weight, time)
                }
                ActivityManager.finishAll()
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                Toast.makeText(this, "Inserire almeno una serie", Toast.LENGTH_SHORT).show()
            }
        }

    }
}