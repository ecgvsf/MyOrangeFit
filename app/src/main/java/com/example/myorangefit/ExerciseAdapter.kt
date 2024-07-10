package com.example.myorangefit

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView


class ExerciseAdapter(private val context: Context, private val exerciseList: List<Workout>, private val date : String) :
    RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val (id, name, _, type, image) = exerciseList[position]

        // Setta il nome dell'esercizio
        holder.textViewExerciseName.text = name

        // Carica l'immagine
        //holder.imageViewExercise.setImageResource(image)

        //listener per selezione dell'esercizio
        holder.cardExercise.setOnClickListener {
            var intent = Intent()
            intent = if (type == 1){ // 0 tempo, 1 peso
                Intent(context, WeightExerciseActivity::class.java)
            } else {
                Intent(context, TimeExerciseActivity::class.java)
            }
            intent.putExtra("date", date)
            intent.putExtra("id_workout", id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return exerciseList.size
    }

    class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageViewExercise: ImageView
        var textViewExerciseName: TextView
        val cardExercise: CardView

        init {
            cardExercise = itemView.findViewById<CardView>(R.id.card)
            imageViewExercise = itemView.findViewById<ImageView>(R.id.cardView)
            textViewExerciseName = itemView.findViewById<TextView>(R.id.textView)
        }
    }
}
