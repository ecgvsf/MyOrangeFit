package com.example.myorangefit.adapter

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
import com.example.myorangefit.R
import com.example.myorangefit.activity.AddWorkoutTypeActivity
import com.example.myorangefit.model.Workout

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
        private val trashView: CardView = itemView.findViewById(R.id.trashCard)
        private val editView: CardView = itemView.findViewById(R.id.editCard)

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

            editView.setOnClickListener {
                val intent = Intent(itemView.context, AddWorkoutTypeActivity::class.java)
                intent.putExtra("editFlag", 1)
                intent.putExtra("bodyPart", workout.bodyPart)
                intent.putExtra("id", workout.id)
                itemView.context.startActivity(intent)
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
