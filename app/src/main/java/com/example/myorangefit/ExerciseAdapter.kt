package com.example.myorangefit

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class ExerciseAdapter(private val context: Context, private val exerciseList: List<Workout>, private val date : String) :
    RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val (id, _, _, type, image) = exerciseList[position]

        // Setta il nome dell'esercizio
        val databaseHelper = DatabaseHelper(context)
        val name = databaseHelper.getNameByIdWorkout(id)
        holder.textViewExerciseName.text = name

        // Carica l'immagine
        if (image.isNotEmpty()) {
            Glide.with(context)
                .load(image)
                .centerCrop()
                .into(holder.imageViewExercise)
            val layoutParams = holder.imageViewExercise.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(0, 0, 0, 0)
            holder.imageViewExercise.layoutParams = layoutParams
            holder.imageViewExercise.setColorFilter(Color.TRANSPARENT)
        }

        //listener per selezione dell'esercizio
        holder.cardViewExercise.setOnClickListener {
            var intent = Intent()
            intent = Intent(context, SeriesActivity::class.java)
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
        var cardViewExercise: CardView

        init {
            cardViewExercise = itemView.findViewById<CardView>(R.id.card)
            imageViewExercise = itemView.findViewById<ImageView>(R.id.image)
            textViewExerciseName = itemView.findViewById<TextView>(R.id.textView)
        }
    }
}
