package com.example.myorangefit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class SeriesAdapter(private val seriesList: MutableList<Pair<Float, Int>>) :
    RecyclerView.Adapter<SeriesAdapter.SeriesViewHolder>() {

    class SeriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val series: TextView = itemView.findViewById(R.id.serie)
        val reps: TextView = itemView.findViewById(R.id.reps)
        val weight: TextView = itemView.findViewById(R.id.weight)
        val deleteButton: CardView = itemView.findViewById(R.id.trash)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_serie, parent, false)
        return SeriesViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        val series = seriesList[position]

        holder.series.text = "Series ${position + 1}"
        if (series.first != -7f) {
            holder.reps.text = "Reps: ${series.second}"
            holder.weight.text = "${series.first.toInt()} (kg)"
        } else {
            val minutes = series.second / 60
            val seconds = series.second - (minutes * 60)
            holder.reps.text = " "
            holder.weight.text = "$minutes m e $seconds s"
        }

        holder.deleteButton.setOnClickListener {
            seriesList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, seriesList.size)
            updateSeriesNumbers()
        }
    }

    override fun getItemCount(): Int {
        return seriesList.size
    }

    // Metodo per aggiornare i numeri delle serie dopo l'eliminazione di un elemento
    private fun updateSeriesNumbers() {
        for (i in seriesList.indices) {
            notifyItemChanged(i)
        }
    }
}
