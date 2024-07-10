package com.example.myorangefit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.cardview.widget.CardView

class BodyPartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityManager.add(this)
        setContentView(R.layout.activity_body_part)

        // Esempio per la card del petto
        val chestCard: CardView = findViewById(R.id.chest)
        val backCard: CardView = findViewById(R.id.back)
        val shoulderCard: CardView = findViewById(R.id.shoulder)
        val armCard: CardView = findViewById(R.id.arm)
        val absCard: CardView = findViewById(R.id.abs)
        val legCard: CardView = findViewById(R.id.leg)

        val calendar = intent.getIntExtra("calendar", -1)
        var newIntent = Intent(this, MainActivity::class.java)
        if (calendar == 0) //l'activity che ha lanciato questa activity è ManageWorkout
            newIntent = Intent(this, AddWorkoutTypeActivity::class.java)
        else if (calendar == 1) { //l'activity che ha lanciato questa activity è Main
            val selectedDate = intent.getStringExtra("selectedDate")
            newIntent = Intent(this, ExerciseSelectionActivity::class.java)
            newIntent.putExtra("selectedDate", selectedDate)
        }

        chestCard.setOnClickListener {
            newIntent.putExtra("bodyPart", "Chest")
            startActivity(newIntent)
        }
        backCard.setOnClickListener {
            newIntent.putExtra("bodyPart", "Back")
            startActivity(newIntent)
        }
        shoulderCard.setOnClickListener {
            newIntent.putExtra("bodyPart", "Shoulders")
            startActivity(newIntent)
        }
        armCard.setOnClickListener {
            newIntent.putExtra("bodyPart", "Arms")
            startActivity(newIntent)
        }
        absCard.setOnClickListener {
            newIntent.putExtra("bodyPart", "Abs")
            startActivity(newIntent)
        }
        legCard.setOnClickListener {
            newIntent.putExtra("bodyPart", "Legs")
            startActivity(newIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
    }
}
