package com.example.myorangefit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.example.myorangefit.databinding.ActivityManageWorkoutBinding

class ManageWorkoutActivity : AppCompatActivity(), OnWorkoutDeleteListener {

    private lateinit var binding: ActivityManageWorkoutBinding
    private lateinit var databaseHelper: DatabaseHelper

    private lateinit var workoutsByBodyPart: Map<String, List<Workout>>
    private lateinit var workoutAdapters: MutableList<WorkoutAdapter>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityManager.add(this)
        binding = ActivityManageWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelperSingleton.getInstance(this)
        workoutAdapters = mutableListOf()


        // Carica gli allenamenti dal database
        loadWorkoutsFromDatabase()

        // Rendi visibili le liste di esercizi
        binding.armsArrow.setOnClickListener { changeVisibility("arms") }
        binding.absArrow.setOnClickListener { changeVisibility("abs") }
        binding.shouldersArrow.setOnClickListener { changeVisibility("shoulders") }
        binding.backArrow.setOnClickListener { changeVisibility("back") }
        binding.chestArrow.setOnClickListener { changeVisibility("chest") }
        binding.legsArrow.setOnClickListener { changeVisibility("legs") }

        // Gestisci il clic sul pulsante di aggiunta di un nuovo tipo di allenamento
        binding.fab.setOnClickListener {
            val intent = Intent(this, BodyPartActivity::class.java)
            intent.putExtra("calendar", 0)
            startActivity(intent)
        }
    }

    private fun loadWorkoutsFromDatabase() {
        // Ottieni tutti gli allenamenti per ogni parte del corpo dal database
        workoutsByBodyPart = databaseHelper.getAllWorkoutsByBodyPart()

        // Inserisci gli allenamenti nelle rispettive RecyclerView
        populateRecyclerView(binding.armsList, workoutsByBodyPart["Arms"] ?: emptyList())
        populateRecyclerView(binding.absList, workoutsByBodyPart["Abs"] ?: emptyList())
        populateRecyclerView(binding.shouldersList, workoutsByBodyPart["Shoulders"] ?: emptyList())
        populateRecyclerView(binding.backList, workoutsByBodyPart["Back"] ?: emptyList())
        populateRecyclerView(binding.chestList, workoutsByBodyPart["Chest"] ?: emptyList())
        populateRecyclerView(binding.legsList, workoutsByBodyPart["Legs"] ?: emptyList())
    }

    private fun populateRecyclerView(recyclerView: RecyclerView, workouts: List<Workout>) {
        recyclerView.layoutManager = LinearLayoutManager(this)
        val workoutAdapter = WorkoutAdapter(workouts.toMutableList(), this)
        recyclerView.adapter = workoutAdapter
        workoutAdapters.add(workoutAdapter)
    }

    private fun changeVisibility(bodyPart: String) {
        // Ottieni l'id della RecyclerView corrispondente alla parte del corpo
        val recyclerViewId = resources.getIdentifier("${bodyPart}_list", "id", packageName)
        val recyclerView = findViewById<RecyclerView>(recyclerViewId)

        // Ottieni l'id dell'ImageButton corrispondente alla parte del corpo
        val imageButtonId = resources.getIdentifier("${bodyPart}_arrow", "id", packageName)
        val imageButton = findViewById<ImageButton>(imageButtonId)

        val v: Int = if (recyclerView.visibility == View.GONE)
            View.VISIBLE
        else
            View.GONE
        TransitionManager.beginDelayedTransition(binding.waterLayout, AutoTransition())
        recyclerView.visibility = v
        val i: Int = if (recyclerView.visibility != View.GONE)
            R.drawable.ic_up
        else
            R.drawable.ic_down
        imageButton.setImageDrawable(ContextCompat.getDrawable(this, i))
    }

    override fun onDelete(workout: Workout) {
        val isDeleted = databaseHelper.deleteWorkout(workout.id)
        if (isDeleted) {
            //Toast.makeText(this, "Esercizio eliminato con successo", Toast.LENGTH_SHORT).show()

            // Trova l'adapter che contiene il workout da eliminare
            val adapterToRemove = workoutAdapters.find { it.containsWorkout(workout) }
            if (adapterToRemove != null) {
                adapterToRemove.removeWorkout(workout)
            } else {
                //Toast.makeText(this, "Adapter non trovato per l'eliminazione dell'esercizio", Toast.LENGTH_SHORT).show()
            }
        } else {
            //Toast.makeText(this, "Errore nell'eliminazione dell'esercizio", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
    }
}
