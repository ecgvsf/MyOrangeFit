package com.example.myorangefit.fragment

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.DatePicker
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myorangefit.R
import com.example.myorangefit.compose.ComposeBarChart
import com.example.myorangefit.compose.ComposeLineChart
import com.example.myorangefit.compose.PlaceHolderText
import com.example.myorangefit.database.DatabaseHelper
import com.example.myorangefit.databinding.FragmentStatisticBinding
import com.example.myorangefit.xml.SquareFrameLayout
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.coroutines.launch
import java.time.LocalDate

class StatisticFragment : Fragment() {

    private var _binding : FragmentStatisticBinding? = null
    private val binding get() = _binding!!

    private val modelProducer1 = CartesianChartModelProducer()
    private val modelProducer2 = CartesianChartModelProducer()
    private val modelProducer3 = CartesianChartModelProducer()
    private val modelProducer4 = CartesianChartModelProducer()
    private val modelProducer5 = CartesianChartModelProducer()
    private val modelProducer6 = CartesianChartModelProducer()

    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseHelper = DatabaseHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentStatisticBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val extraChart = listOf(
            Pair(binding.composeChart1, binding.title1),
            Pair(binding.composeChart2, binding.title2),
            Pair(binding.composeChart3, binding.title3),
            Pair(binding.composeChart4, binding.title4),
            Pair(binding.composeChart5, binding.title5),
            Pair(binding.composeChart6, binding.title6)
        )

        for ((i, pair) in extraChart.withIndex()) {
            pair.first.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    PlaceHolderText("Clicca Per Impostare il Grafico")
                }
            }
            pair.first.setOnClickListener {
                showChooseChartDialog(pair.first, pair.second)
            }
        }


        showChart(binding.composeChart, binding.textMainChart, 2, 4, "line")

        binding.week.setOnClickListener {
            binding.week.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            binding.month.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.year.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            //aggiorna grafici
            showChart(binding.composeChart, binding.textMainChart, 0, 4, "line")
        }

        binding.month.setOnClickListener {
            binding.week.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.month.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            binding.year.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            //aggiorna grafici
            showChart(binding.composeChart, binding.textMainChart, 1, 4, "line")
        }

        binding.year.setOnClickListener {
            binding.week.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.month.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.year.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))

            //aggiorna grafici
            showChart(binding.composeChart, binding.textMainChart, 2, 4, "line")
        }
    }

    private fun showChart(chart: ComposeView, text: TextView, dateFilter: Int, id: Int, type: String) {
        val today = LocalDate.now()
        val list = databaseHelper.getWorkoutsCalendarWeightById(id, dateFilter, today)

        if (list.isEmpty()) {
            val period = when (dateFilter) {
                0 -> "Last Week"
                1 -> "Last Month"
                2 -> "Last Year"
                else -> ""
            }

            chart.visibility = View.GONE
            text.visibility = View.VISIBLE
            text.text = "No Workout Done " + period
        } else {
            chart.visibility = View.VISIBLE
            text.visibility = View.GONE
            chart.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    if (type == "line")
                        ComposeLineChart(list)
                    else if (type == "bar")
                        ComposeBarChart(list)
                    else if (type == "pie")
                        PlaceHolderText("non implementato") //TO DO
                    else
                        PlaceHolderText("Grafico non disponibile")
                }
            }
        }
    }


    private fun showChooseChartDialog(chart: ComposeView, text: TextView) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_chart)


        dialog.findViewById<SquareFrameLayout>(R.id.lineChart).setOnClickListener {
            showChart(chart, text, 2, 4, "line")
            dialog.dismiss()
        }

        dialog.findViewById<SquareFrameLayout>(R.id.barChart).setOnClickListener {
            showChart(chart, text, 2, 4, "bar")
            dialog.dismiss()
        }

        dialog.findViewById<SquareFrameLayout>(R.id.pieChart).setOnClickListener {
            showChart(chart, text, 2, 4, "pie")
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }
}