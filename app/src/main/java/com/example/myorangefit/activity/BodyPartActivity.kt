package com.example.myorangefit.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.myorangefit.R


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

        val flag = intent.getIntExtra("flag", -1)   // 0 : crea un nuovo allenamento,
                                                                    // 1 : aggiungi un allenamento nel calendario,
                                                                    // 2 : modifica un allenamento

        chestCard.setOnClickListener (
            ClickListener("Chest", flag)
        )
        backCard.setOnClickListener (
            ClickListener("Back", flag)
        )
        shoulderCard.setOnClickListener (
            ClickListener("Shoulders", flag)
        )
        armCard.setOnClickListener (
            ClickListener("Arms", flag)
        )
        absCard.setOnClickListener (
            ClickListener("Abs", flag)
        )
        legCard.setOnClickListener (
            ClickListener("Legs", flag)
        )
    }

    private class ClickListener(private val bodyPart: String, private val flag: Int) : OnClickListener {
        override fun onClick(v: View) {
            val context = v.context
            Log.d("typee", "$flag")

            if (flag == 2) {
                val resultIntent = Intent()
                resultIntent.putExtra("bodyPart", bodyPart)

                // Controlla se il contesto è un'istanza di Activity
                if (context is Activity) {
                    context.setResult(Activity.RESULT_OK, resultIntent)
                    context.finish()
                }
            } else {
                var newIntent: Intent? = null
                if (flag == 0) {
                    // l'activity che ha lanciato questa activity è ManageWorkout
                    newIntent = Intent(context, AddWorkoutTypeActivity::class.java)
                } else if (flag == 1) {
                    // l'activity che ha lanciato questa activity è Main
                    if (context is Activity) {
                        val selectedDate = context.intent.getStringExtra("selectedDate")
                        newIntent = Intent(context, ExerciseSelectionActivity::class.java)
                        newIntent.putExtra("selectedDate", selectedDate)
                    }
                }

                newIntent?.putExtra("bodyPart", bodyPart)
                newIntent?.let { context.startActivity(it) }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
    }
}
