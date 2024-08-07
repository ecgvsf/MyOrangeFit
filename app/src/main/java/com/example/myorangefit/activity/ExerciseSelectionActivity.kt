package com.example.myorangefit.activity

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myorangefit.R
import com.example.myorangefit.adapter.ExerciseAdapter
import com.example.myorangefit.database.DatabaseHelper
import com.example.myorangefit.database.DatabaseHelperSingleton
import com.example.myorangefit.model.Workout


class ExerciseSelectionActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var title: TextView
    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityManager.add(this)
        setContentView(R.layout.activity_exercise_selection)

        val selectedDate = intent.getStringExtra("selectedDate").orEmpty()
        val bodyPart = intent.getStringExtra("bodyPart")

        recyclerView = findViewById(R.id.recyclerViewExercise)
        title = findViewById(R.id.title)
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (bodyPart == null) {
            // Gestisci il caso di errore, ad esempio mostra un messaggio di errore e termina l'Activity
            Toast.makeText(this, "Body part not provided", Toast.LENGTH_SHORT).show()
            finish() // Termina l'Activity
            return
        }

        databaseHelper = DatabaseHelperSingleton.getInstance(this)
        val exerciseList: List<Workout> = databaseHelper.getWorkoutsByBodyPart(bodyPart)

        title.text = bodyPart

        exerciseAdapter = ExerciseAdapter(this, exerciseList, selectedDate)
        recyclerView.adapter = exerciseAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
    }
}
