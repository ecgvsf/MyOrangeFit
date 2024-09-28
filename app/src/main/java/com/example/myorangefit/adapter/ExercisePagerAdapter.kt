package com.example.myorangefit.adapter

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myorangefit.database.DatabaseHelper
import com.example.myorangefit.database.DatabaseHelperSingleton
import com.example.myorangefit.model.Workout
import java.time.LocalDate

class ExercisePagerAdapter(
    fragmentActivity: FragmentActivity,
    private val exerciseList: MutableList<MutableList<Workout>>,
    private val date: LocalDate,
    private var isWeekMode: Boolean
) : FragmentStateAdapter(fragmentActivity) {
    // Mappa per memorizzare i fragment associati a ogni posizione
    private val fragmentMap = mutableMapOf<Int, ExerciseFragment>()
    private lateinit var databaseHelper: DatabaseHelper

    override fun getItemCount(): Int {
        return exerciseList.size  // Numero di pagine
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = ExerciseFragment()
        fragment.arguments = Bundle().apply {
            putParcelableArrayList("exercises", ArrayList(exerciseList[position]))
            putString("date", date.toString())
            putBoolean("week", isWeekMode)
        }

        // Memorizza il fragment creato
        fragmentMap[position] = fragment

        return fragment
    }

    // Metodo per ottenere il fragment associato a una determinata posizione
    fun getFragmentAt(position: Int): ExerciseFragment? {
        return fragmentMap[position]
    }

    fun setSwipe(swipe: Boolean) {
        fragmentMap.forEach { (_, fragment) -> fragment.setSwipe(swipe) }
    }

    fun removeAndShiftItem(): Boolean{
        Log.e("sss", "$exerciseList")
        for (i in (fragmentMap.size - 1) downTo 0) {
            val adapter = fragmentMap[i]?.getAdapter()
            adapter?.let {
                val selected = adapter.getSelectedItems()
                Log.e("SSS", "$selected")
                if (selected.isNotEmpty()) {
                    selected.sortedByDescending { it.first }.forEach { pair ->
                        val selectedIndex = pair.first

                        // Rimuovi l'elemento dalla lista e dall'adapter
                        if (selectedIndex < exerciseList[i].size) {
                            val removedExercise = exerciseList[i][selectedIndex]
                            adapter.removeItem(selectedIndex)
                            exerciseList[i].removeAt(selectedIndex)
                            fragmentMap[i]?.removeItemFromDB(pair.second)

                            // Se non sei sull'ultima lista e l'ultima lista non è vuota
                            if (i != fragmentMap.size - 1 && exerciseList.last().isNotEmpty()) {
                                // Prendi l'ultimo elemento dalla lista finale
                                val lastItem = exerciseList.last().last()

                                // Aggiungi l'ultimo elemento alla lista corrente
                                exerciseList[i].add(lastItem)
                                exerciseList.last().removeAt(exerciseList.last().lastIndex)

                                // Aggiorna l'adapter della lista corrente
                                adapter.addItem(lastItem)
                                adapter.notifyItemInserted(exerciseList[i].size - 1)

                                // Aggiorna l'adapter della lista finale
                                val lastAdapter = fragmentMap[fragmentMap.size - 1]?.getAdapter()
                                lastAdapter?.let {
                                    lastAdapter.removeItem(exerciseList.last().size) // Rimuovi l'ultimo elemento
                                    lastAdapter.notifyItemRemoved(exerciseList.last().size)
                                }
                            }
                        }
                        // Se l'ultima lista diventa vuota, rimuovila
                        if (exerciseList.lastOrNull()?.isEmpty() == true) {
                            exerciseList.removeAt(exerciseList.lastIndex)
                            fragmentMap.remove(fragmentMap.size - 1)
                            notifyItemRemoved(fragmentMap.size)  // Notifica l'adapter del ViewPager
                        }
                    }
                }
            }

        }
        Log.e("sss", "$exerciseList")
        return exerciseList.isEmpty()
    }

    fun toggleSelection(position: Int, shouldSelection: Boolean) {
       val recyclerView = fragmentMap[position]?.getRecyclerView()

        recyclerView?.let {
            val adapter = recyclerView.adapter as? ExerciseCalendarAdapter
            adapter?.let {
                // Itera sugli item visibili del RecyclerView
                for (i in 0 until recyclerView.childCount) {
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as? ExerciseCalendarAdapter.ExerciseCalendarViewHolder
                    viewHolder?.let { holder ->
                        // Chiama toggleExpansion per ciascun item visibile
                        adapter.toggleSelection(holder, shouldSelection)
                    }
                }
            }
        }
    }

    fun toggleExpansion(position: Int, shouldExpand: Boolean) {
        val recyclerView = fragmentMap[position]?.getRecyclerView()
        recyclerView?.let {
            val adapter = recyclerView.adapter as? ExerciseCalendarAdapter
            // Verifica se l'adapter non è null
            adapter?.let {
                // Itera sugli item visibili del RecyclerView
                for (i in 0 until recyclerView.childCount) {
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as? ExerciseCalendarAdapter.ExerciseCalendarViewHolder
                    viewHolder?.let { holder ->
                        // Chiama toggleExpansion per ciascun item visibile
                        adapter.toggleExpansion(holder, shouldExpand)
                    } ?: run {
                        adapter.notifyItemChanged(i)
                    }
                }
            }
        }
    }
}

