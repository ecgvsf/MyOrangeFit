package com.example.myorangefit.adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myorangefit.R
import com.example.myorangefit.model.Workout

class ExerciseCalendarAdapter(
    private val context: Context,
    private var exerciseList: List<Workout> // Nota che qui usiamo `var` per poter aggiornare la lista
) : RecyclerView.Adapter<ExerciseCalendarAdapter.ExerciseCalendarViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseCalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exercise, parent, false)
        return ExerciseCalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseCalendarViewHolder, position: Int) {
        Log.d("morona", "$exerciseList")
        val (id, name, _, type, image) = exerciseList[position]

        // Setta il nome dell'esercizio
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

    }

    override fun getItemCount() = exerciseList.size

    // Metodo per aggiornare i dati dell'adapter
    fun updateData(newExerciseList: List<Workout>) {
        exerciseList = newExerciseList
        notifyDataSetChanged()  // Notifica al RecyclerView che i dati sono cambiati
    }

    inner class ExerciseCalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageViewExercise: ImageView = itemView.findViewById(R.id.image)
        var textViewExerciseName: TextView = itemView.findViewById(R.id.textView)
        var cardViewExercise: CardView = itemView.findViewById(R.id.card)
    }
}
