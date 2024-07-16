package com.example.myorangefit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity


class SeriesActivity : AppCompatActivity() {

    private lateinit var weightActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var timeActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series)

        databaseHelper = DatabaseHelper(this)

        val data = intent.getStringExtra("date").orEmpty()
        val id = intent.getIntExtra("id_workout", -1)
        val workout = databaseHelper.getWorkoutById(id)

        Log.d("cazzo", workout?.name.toString())
        Log.d("cazzo", workout?.id.toString())
        Log.d("cazzo", id.toString())

        val title = findViewById<TextView>(R.id.exercise_title)
        title.text = workout?.name

        //intent.putExtra("date", date)
        //intent.putExtra("id_workout", id)

        weightActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val resultData = data.getStringExtra("result")
                    // Aggiorna la UI con il risultato della Scale Activity

                }
            }
        }

        timeActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val resultData = data.getStringExtra("result")
                    // Aggiorna la UI con il risultato dell'Another Activity

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

    }
}