package com.example.myorangefit.adapter

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myorangefit.R
import com.example.myorangefit.activity.SeriesActivity
import com.example.myorangefit.database.DatabaseHelper
import com.example.myorangefit.model.Workout
import java.time.LocalDate

class ExerciseCalendarAdapter(
    private val context: Context,
    private var exerciseList: MutableList<Workout>,
    private val date: LocalDate,
    private var isWeekMode: Boolean
) : RecyclerView.Adapter<ExerciseCalendarAdapter.ExerciseCalendarViewHolder>() {

    private val selectedItems = mutableSetOf<Pair<Int, Int>>()

    // Aggiungi un metodo per aggiornare lo stato dell'espansione
    fun setExpandedMode(expanded: Boolean) {
        isWeekMode = expanded
        notifyDataSetChanged() // Forza il ridisegno degli item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseCalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_exercise, parent, false)
        return ExerciseCalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseCalendarViewHolder, position: Int) {
        Log.d("morona", "$exerciseList")
        val (id, name, _, type, image) = exerciseList[position]

        val databaseHelper = DatabaseHelper(context)
        val (series, reps, weight) = databaseHelper.getSeriesByWorkoutnDate(id, date)

        holder.checkBoxLayout.setOnClickListener {
            val isChecked = holder.checkBoxView.isChecked
            if (isChecked) {
                selectedItems.add(Pair(position, id))
            } else {
                selectedItems.remove(Pair(position, id))
            }
            holder.checkBoxView.setChecked(!isChecked)
        }

        holder.checkBoxView.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedItems.add(Pair(position, id))
            } else {
                selectedItems.remove(Pair(position, id))
            }
        }

        // Setta il nome dell'esercizio
        holder.textViewExerciseName.text = name
        holder.seriesView.text = "Series tot: $series"
        holder.repsView.text = "Avarage reps: $reps"
        holder.weightView.text = "Avarage weight: $weight"

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

        holder.checkBoxView.isChecked = false
        holder.checkBoxView.visibility = View.GONE

        if (isWeekMode) {
            val imageL = holder.cardView.layoutParams as ConstraintLayout.LayoutParams
            imageL.startToStart = holder.itemView.id
            imageL.topToTop = holder.itemView.id
            imageL.height = 80.dpToPx(context)
            imageL.width = 80.dpToPx(context)
            holder.cardView.layoutParams = imageL
            holder.cardView.radius = 24.dpToPx(context).toFloat()

            val lineL = holder.lineView.layoutParams as ConstraintLayout.LayoutParams
            lineL.startToStart = holder.cardView.id
            lineL.marginStart = 88.dpToPx(context)
            lineL.topToTop = holder.cardView.id
            lineL.topMargin = 5.dpToPx(context)
            lineL.height = 70.dpToPx(context)
            lineL.width = 3.dpToPx(context)
            holder.lineView.layoutParams = lineL

            holder.seriesView.visibility = View.VISIBLE
            holder.repsView.visibility = View.VISIBLE
            holder.weightView.visibility = View.VISIBLE
        } else {
            val imageL = holder.cardView.layoutParams as ConstraintLayout.LayoutParams
            imageL.startToStart = holder.itemView.id
            imageL.topToTop = holder.itemView.id
            imageL.height = 40.dpToPx(context)
            imageL.width = 40.dpToPx(context)
            holder.cardView.layoutParams = imageL
            holder.cardView.radius = 12.dpToPx(context).toFloat()

            val lineL = holder.lineView.layoutParams as ConstraintLayout.LayoutParams
            lineL.startToStart = holder.cardView.id
            lineL.marginStart = 58.dpToPx(context)
            lineL.topToTop = holder.textViewExerciseName.id
            lineL.topMargin = 30.dpToPx(context)
            lineL.height = 3.dpToPx(context)
            lineL.width = 150.dpToPx(context)
            holder.lineView.layoutParams = lineL

            holder.seriesView.visibility = View.GONE
            holder.repsView.visibility = View.GONE
            holder.weightView.visibility = View.GONE
        }
    }

    fun toggleSelection(holder: ExerciseCalendarViewHolder, shouldSelected: Boolean) {

        val animatorSet = AnimatorSet()

        if (shouldSelected) {
            val fadeOut = ObjectAnimator.ofFloat(holder.checkBoxView, "alpha", 1f, 0f)
            fadeOut.duration = 300
            // Aggiungi un listener per impostare la visibilità su GONE dopo l'animazione
            fadeOut.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    // Solo quando l'animazione è terminata, imposta la visibilità su GONE
                    holder.checkBoxView.visibility = View.GONE
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}
            })
            animatorSet.play(fadeOut)
        } else {
            holder.checkBoxView.isChecked = false
            holder.checkBoxView.visibility = View.VISIBLE
            val fadeIn = ObjectAnimator.ofFloat(holder.checkBoxView, "alpha", 0f, 1f)
            fadeIn.duration = 300
            animatorSet.play(fadeIn)
        }

        animatorSet.start()
    }

    // Funzione per espandere o collassare la vista
    fun toggleExpansion(holder: ExerciseCalendarViewHolder, shouldExpand: Boolean) {
        if (!shouldExpand) {
            collapseView(holder)
        } else {
            expandView(holder)
        }

        isWeekMode = shouldExpand
    }

    // Espande la vista con animazione
    private fun expandView(holder: ExerciseCalendarViewHolder) {
        // Definisci il valore iniziale e finale dell'altezza e della larghezza
        val startWidth = holder.cardView.width
        val startHeight = holder.cardView.height
        val endWidth = 80.dpToPx(context)
        val endHeight = 80.dpToPx(context)

        val widthAnimator = ValueAnimator.ofInt(startWidth, endWidth).apply { duration = 300 }
        val heightAnimator = ValueAnimator.ofInt(startHeight, endHeight).apply { duration = 300 }

        widthAnimator.addUpdateListener { valueAnimator ->
            val animatedWidth = valueAnimator.animatedValue as Int
            holder.cardView.layoutParams.width = animatedWidth
            holder.cardView.requestLayout()
            holder.cardView.radius = 24.dpToPx(context).toFloat()
        }

        heightAnimator.addUpdateListener { valueAnimator ->
            val animatedHeight = valueAnimator.animatedValue as Int
            holder.cardView.layoutParams.height = animatedHeight
            holder.cardView.requestLayout()
        }

        // Animazione della linea
        val startLineWidth = holder.lineView.width
        val endLineWidth = 3.dpToPx(context)
        val lineWidthAnimator = ValueAnimator.ofInt(startLineWidth, endLineWidth).apply {
            addUpdateListener { valueAnimator ->
                val animatedWidth = valueAnimator.animatedValue as Int
                val layoutParams = holder.lineView.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.width = animatedWidth
                holder.lineView.layoutParams = layoutParams
            }
            duration = 225
            doOnStart {
                val layoutParams = holder.lineView.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.startToStart = holder.cardView.id
                layoutParams.marginStart = 88.dpToPx(context)
                holder.lineView.layoutParams = layoutParams
            }
        }

        val startLineHeight = holder.lineView.height
        val endLineHeight = 70.dpToPx(context)
        val lineHeightAnimator = ValueAnimator.ofInt(startLineHeight, endLineHeight).apply {
            addUpdateListener { valueAnimator ->
                val animatedHeight = valueAnimator.animatedValue as Int
                val layoutParams = holder.lineView.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.height = animatedHeight
                holder.lineView.layoutParams = layoutParams
            }
            duration = 75
            doOnStart {
                val layoutParams = holder.lineView.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.topToTop = holder.cardView.id
                layoutParams.topMargin = 5.dpToPx(context)
                holder.lineView.layoutParams = layoutParams
            }
        }

        // Animazione della visibilità degli elementi extra (serie, ripetizioni)
        val fadeIn = ObjectAnimator.ofFloat(holder.seriesView, "alpha", 0f, 1f)
        val fadeInReps = ObjectAnimator.ofFloat(holder.repsView, "alpha", 0f, 1f)
        val fadeInWeight = ObjectAnimator.ofFloat(holder.weightView, "alpha", 0f, 1f)
        fadeIn.duration = 200
        fadeInReps.duration = 150
        fadeInWeight.duration = 100

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(widthAnimator, heightAnimator)
        animatorSet.playSequentially(lineWidthAnimator, lineHeightAnimator)
        animatorSet.playSequentially(fadeIn, fadeInReps, fadeInWeight)
        animatorSet.start()

        // Rendi visibili gli elementi aggiuntivi
        holder.seriesView.visibility = View.VISIBLE
        holder.repsView.visibility = View.VISIBLE
        holder.weightView.visibility = View.VISIBLE
    }


    // Riduci la vista con animazione
    private fun collapseView(holder: ExerciseCalendarViewHolder) {
        val startWidth = holder.cardView.width
        val startHeight = holder.cardView.height
        val endWidth = 40.dpToPx(context)
        val endHeight = 40.dpToPx(context)

        val widthAnimator = ValueAnimator.ofInt(startWidth, endWidth).apply { duration = 300 }
        val heightAnimator = ValueAnimator.ofInt(startHeight, endHeight).apply { duration = 300 }

        widthAnimator.addUpdateListener { valueAnimator ->
            val animatedWidth = valueAnimator.animatedValue as Int
            holder.cardView.layoutParams.width = animatedWidth
            holder.cardView.requestLayout()
            holder.cardView.radius = 12.dpToPx(context).toFloat()
        }

        heightAnimator.addUpdateListener { valueAnimator ->
            val animatedHeight = valueAnimator.animatedValue as Int
            holder.cardView.layoutParams.height = animatedHeight
            holder.cardView.requestLayout()
        }

        // Animazione della linea
        val startLineWidth = holder.lineView.width
        val endLineWidth = 150.dpToPx(context)
        val lineWidthAnimator = ValueAnimator.ofInt(startLineWidth, endLineWidth).apply {
            addUpdateListener { valueAnimator ->
                val animatedWidth = valueAnimator.animatedValue as Int
                val layoutParams = holder.lineView.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.width = animatedWidth
                holder.lineView.layoutParams = layoutParams
            }
            duration = 225
            doOnStart {
                val layoutParams = holder.lineView.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.startToStart = holder.textViewExerciseName.id
                layoutParams.marginStart = 0.dpToPx(context)
                holder.lineView.layoutParams = layoutParams
            }
        }

        val startLineHeight = holder.lineView.height
        val endLineHeight = 3.dpToPx(context)
        val lineHeightAnimator = ValueAnimator.ofInt(startLineHeight, endLineHeight).apply {
            addUpdateListener { valueAnimator ->
                val animatedHeight = valueAnimator.animatedValue as Int
                val layoutParams = holder.lineView.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.height = animatedHeight
                holder.lineView.layoutParams = layoutParams
            }
            duration = 75
            doOnEnd {
                val layoutParams = holder.lineView.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.topToTop = holder.textViewExerciseName.id
                layoutParams.topMargin = 30.dpToPx(context)
                holder.lineView.layoutParams = layoutParams
                holder.lineView.requestLayout() // Forza il layout
            }
        }

        // Animazione della visibilità degli elementi extra (serie, ripetizioni)
        val fadeOut = ObjectAnimator.ofFloat(holder.seriesView, "alpha", 1f, 0f)
        val fadeOutReps = ObjectAnimator.ofFloat(holder.repsView, "alpha", 1f, 0f)
        val fadeOutWeight = ObjectAnimator.ofFloat(holder.weightView, "alpha", 1f, 0f)
        fadeOut.duration = 100
        fadeOutReps.duration = 100
        fadeOutWeight.duration = 100

        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(fadeOutWeight, fadeOutReps, fadeOut)
        animatorSet.playSequentially(lineHeightAnimator, lineWidthAnimator)
        animatorSet.playTogether(widthAnimator, heightAnimator)
        animatorSet.start()

        // Nascondi gli elementi aggiuntivi alla fine dell'animazione
        fadeOut.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                holder.seriesView.visibility = View.GONE
                holder.repsView.visibility = View.GONE
                holder.weightView.visibility = View.GONE
            }

            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }


    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    fun addItem(w: Workout) {
        exerciseList.add(w)
        notifyItemInserted(exerciseList.size - 1)
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < exerciseList.size) {
            exerciseList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, exerciseList.size) // Aggiorna gli altri elementi
        }
    }

    override fun getItemCount() = exerciseList.size

    fun getSelectedItems(): List<Pair<Int, Int>> {
        val list = selectedItems.toList()
        selectedItems.clear()
        return list
    }

    // Metodo per aggiornare i dati dell'adapter
    fun updateData(newExerciseList: MutableList<Workout>) {
        exerciseList = newExerciseList
        notifyDataSetChanged()  // Notifica al RecyclerView che i dati sono cambiati
    }

    inner class ExerciseCalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageViewExercise: ImageView = itemView.findViewById(R.id.image)
        var textViewExerciseName: TextView = itemView.findViewById(R.id.textView)
        var container: ConstraintLayout = itemView.findViewById(R.id.container)
        var cardView: CardView = itemView.findViewById(R.id.cardView)
        val seriesView: TextView = itemView.findViewById(R.id.series)
        val repsView: TextView = itemView.findViewById(R.id.reps)
        val weightView: TextView = itemView.findViewById(R.id.weight)
        val lineView: View = itemView.findViewById(R.id.line_t)
        val checkBoxView: CheckBox = itemView.findViewById(R.id.checkbox)
        val checkBoxLayout: ConstraintLayout = itemView.findViewById(R.id.checkbox_layout)
    }
}

class FadeItemAnimator : DefaultItemAnimator() {

    override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
        holder?.itemView?.apply {
            alpha = 0f
            animate().alpha(1f).setDuration(300).setListener(null).start()
        }
        return super.animateAdd(holder)
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder?): Boolean {
        holder?.itemView?.apply {
            animate().alpha(0f).setDuration(300).setListener(null).start()
        }
        return super.animateRemove(holder)
    }
}

