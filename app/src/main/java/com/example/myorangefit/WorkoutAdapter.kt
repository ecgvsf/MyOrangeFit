package com.example.myorangefit

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class WorkoutAdapter(
    private val workouts: MutableList<Workout>,
    private val deleteListener: OnWorkoutDeleteListener
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workouts[position]
        holder.bind(workout)
    }

    override fun getItemCount(): Int {
        return workouts.size
    }

    fun containsWorkout(workout: Workout): Boolean {
        return workouts.contains(workout)
    }

    inner class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textView)
        private val imageView: ImageView = itemView.findViewById(R.id.image)
        private val trashView: ImageView = itemView.findViewById(R.id.trash)

        fun bind(workout: Workout) {
            nameTextView.text = workout.name
            if (workout.image.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(workout.image)
                    .centerCrop()
                    .into(imageView)
                val layoutParams = imageView.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.setMargins(0, 0, 0, 0)
                imageView.layoutParams = layoutParams
                imageView.setColorFilter(Color.TRANSPARENT)
            }

            trashView.setOnClickListener {
                deleteListener.onDelete(workout)
            }
        }
    }

    fun removeWorkout(workout: Workout) {
        val position = workouts.indexOf(workout)
        if (position != -1) {
            workouts.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
