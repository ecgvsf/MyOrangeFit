package com.myorangefit.app.fragment

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import com.myorangefit.app.activity.MapsActivity
import com.myorangefit.app.R
import com.myorangefit.app.activity.exercise.ManageWorkoutActivity
import com.myorangefit.app.activity.auth.OptionsActivity
import com.myorangefit.app.activity.exercise.RoutineActivity
import com.myorangefit.app.async.WorkoutViewModel
import com.myorangefit.app.compose.ComposeBarChart
import com.myorangefit.app.compose.WeekCalendar
import com.myorangefit.app.database.DatabaseHelper
import com.myorangefit.app.database.DatabaseHelperSingleton
import com.myorangefit.app.databinding.FragmentHomeBinding
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale


class HomeFragment : Fragment() {

    // ViewBinding
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var contx: Context

    private val today = LocalDate.now()

    private var isFrontVisible = true // Stato per sapere quale lato è visibile
    private lateinit var cardFront : CardView
    private lateinit var cardBack : CardView

    private lateinit var freqChart : ComposeView

    private lateinit var weekCalendarView: ComposeView

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fade = Fade().setDuration(100L)
        enterTransition = fade
        exitTransition  = fade

        contx = requireContext()
        databaseHelper = DatabaseHelperSingleton.getInstance(contx)
        viewModel = ViewModelProvider(requireActivity())[WorkoutViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        weekCalendarView = binding.composeView

        cardFront = binding.front
        cardBack = binding.back

        freqChart = binding.freqChart

        contx = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val today = LocalDate.now()

        //inizializza vista per non fare l'effetto di caricamento in ritardo
        weekCalendarView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WeekCalendar(mutableListOf(""))
            }
        }

        viewModel.streakWorkout.observe(viewLifecycleOwner) { streak ->
            binding.streak.text = "${streak}x ${if (streak == 1) "Day" else "Days"}"
        }

        viewModel.weekWorkout.observe(viewLifecycleOwner) { count ->
            binding.week.text = "$count ${if (count == 1) "Time" else "Times"}"
        }

        viewModel.weekDatesList.observe(viewLifecycleOwner) { list ->
            weekCalendarView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    WeekCalendar(list.mapTo(mutableListOf()){it.first})
                }
            }
        }

        viewModel.weekDatesList.observe(viewLifecycleOwner) { list ->
            freqChart.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    ComposeBarChart(
                        list.toWeeklyQuantities(today, DayOfWeek.MONDAY),
                        labelX = true,
                        labelY = false,
                        guidelineX = true,
                        guidelineY = false
                    )
                }
            }
        }



        binding.workoutCard.setOnClickListener {
            val intent = Intent(contx, ManageWorkoutActivity::class.java)
            startActivity(intent)
        }

        binding.routineCard.setOnClickListener {
            val intent = Intent(contx, RoutineActivity::class.java)
            startActivity(intent)
        }

        binding.runningCard.setOnClickListener {
            val intent = Intent(contx, MapsActivity::class.java)
            startActivity(intent)
        }

        binding.cardContainer.setOnClickListener { /*flipCard()*/ }

        binding.options.setOnClickListener {
            val intent = Intent(contx, OptionsActivity::class.java)
            startActivity(intent)
        }
    }

    fun List<Pair<String, Int>>.toWeeklyQuantities(
        today: LocalDate = LocalDate.now(),
        weekStartsOn: DayOfWeek = DayOfWeek.MONDAY
    ): MutableList<Pair<String, Int>> {
        val quantitiesByDate = this.toMap()
        val startOfWeek = today.with(weekStartsOn)

        return (0L until 7L)
            .map { offset ->
                val date = startOfWeek.plusDays(offset)
                date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH).first().toString() to (quantitiesByDate[date.toString()] ?: 0)
            }
            .toMutableList()
    }

    private fun flipCard() {
        val flipOutAnimatorSet = AnimatorInflater.loadAnimator(contx, R.animator.flip_out) as AnimatorSet
        val flipInAnimatorSet = AnimatorInflater.loadAnimator(contx, R.animator.flip_in) as AnimatorSet

        // Quando l'animazione di flip-out termina, nascondi la vista
        flipOutAnimatorSet.setTarget(if (isFrontVisible) cardFront else cardBack)
        flipInAnimatorSet.setTarget(if (isFrontVisible) cardBack else cardFront)

        flipOutAnimatorSet.start()
        flipInAnimatorSet.start()

        isFrontVisible = !isFrontVisible
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}