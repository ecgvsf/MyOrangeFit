package com.example.myorangefit.adapter

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myorangefit.R
import com.example.myorangefit.activity.SeriesActivity
import com.example.myorangefit.activity.StatisticsWorkoutActivity
import com.example.myorangefit.database.DatabaseHelper
import com.example.myorangefit.database.DatabaseHelperSingleton
import com.example.myorangefit.model.Workout
import java.time.LocalDate
import java.util.Collections

class ExerciseFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExerciseCalendarAdapter
    private var exercises: MutableList<Workout> = mutableListOf()
    private lateinit var date: LocalDate
    private var isWeekMode: Boolean = false

    private var isSwipeEnabled = true

    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        databaseHelper = DatabaseHelperSingleton.getInstance(requireContext())

        val view = inflater.inflate(R.layout.fragment_exercise_list, container, false)
        recyclerView = view.findViewById(R.id.list)

        exercises = arguments?.getParcelableArrayList("exercises") ?: mutableListOf()
        date = LocalDate.parse(arguments?.getString("date"))
        isWeekMode = arguments?.getBoolean("week") ?: false


        recyclerView.layoutManager = object : LinearLayoutManager(requireContext()) {
            override fun canScrollVertically() = false
        }
        adapter = ExerciseCalendarAdapter(requireContext(), exercises, date, isWeekMode)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = FadeItemAnimator()

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP.or(ItemTouchHelper.DOWN), ItemTouchHelper.LEFT.or(ItemTouchHelper.RIGHT)) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // Controlla se lo swipe è abilitato
                if (!isSwipeEnabled) return false

                val startPosition = viewHolder.bindingAdapterPosition
                val endPosition = target.bindingAdapterPosition

                Collections.swap(exercises, startPosition, endPosition)
                recyclerView.adapter?.notifyItemMoved(startPosition, endPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (!isSwipeEnabled) return

                val position = viewHolder.bindingAdapterPosition

                when(direction) {
                    ItemTouchHelper.LEFT -> {
                        // Ripristina l'elemento alla sua posizione originale
                        recyclerView.adapter?.notifyItemChanged(position)

                        val workout = exercises[position]

                        val intent = Intent(requireContext(), SeriesActivity::class.java)
                        intent.putExtra("edit", 1)
                        intent.putExtra("date", date.toString())
                        intent.putExtra("id_workout", workout.id)
                        requireContext().startActivity(intent)
                    }

                    ItemTouchHelper.RIGHT -> {
                        // Ripristina l'elemento alla sua posizione originale
                        recyclerView.adapter?.notifyItemChanged(position)

                        val workout = exercises[position]

                        val intent = Intent(requireContext(), StatisticsWorkoutActivity::class.java)
                        intent.putExtra("id_workout", workout.id)
                        requireContext().startActivity(intent)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val icon = ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_edit) // L'icona da disegnare
                val icon2 = ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_pie_chart) // L'icona da disegnare
                val background = ColorDrawable(resources.getColor(R.color.trasparent)) // Lo sfondo durante lo swipe a sinistra

                val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
                val icon2Margin = (itemView.height - icon2!!.intrinsicHeight) / 2
                val iconTop = itemView.top + iconMargin
                val icon2Top = itemView.top + icon2Margin
                val iconBottom = iconTop + icon.intrinsicHeight
                val icon2Bottom = iconTop + icon2.intrinsicHeight

                if (dX < 0) { // Swipe a sinistra
                    // Disegna lo sfondo rosso
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)

                    // Disegna l'icona a destra
                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    icon.draw(c)
                }

                // Swipe a destra (dX > 0)
                if (dX > 0) {
                    // Disegna lo sfondo a destra
                    background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                    background.draw(c)

                    // Posiziona l'icona a sinistra
                    val icon2Left = itemView.left + icon2Margin
                    val icon2Right = itemView.left + icon2Margin + icon2.intrinsicWidth
                    icon2.setBounds(icon2Left, icon2Top, icon2Right, icon2Bottom)
                    icon2.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)


        return view
    }

    fun removeItemFromDB(id: Int) {
        databaseHelper.deleteWorkoutCalendar(id, date.toString())
    }

    fun setSwipe(swipe: Boolean) {
        isSwipeEnabled = swipe

        Log.e("sss", "$isSwipeEnabled")
    }

    // Aggiungi un metodo per accedere alla RecyclerView
    fun getRecyclerView(): RecyclerView {
        return recyclerView
    }

    fun getAdapter(): ExerciseCalendarAdapter {
        return adapter
    }
}
