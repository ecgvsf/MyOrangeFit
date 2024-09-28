package com.example.myorangefit.fragment

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import com.example.myorangefit.MapsActivity
import com.example.myorangefit.R
import com.example.myorangefit.activity.ManageWorkoutActivity
import com.example.myorangefit.async.WorkoutViewModel
import com.example.myorangefit.compose.WeekCalendar
import com.example.myorangefit.database.DatabaseHelper
import com.example.myorangefit.database.DatabaseHelperSingleton
import com.example.myorangefit.databinding.FragmentHomeBinding
import java.time.LocalDate


class HomeFragment : Fragment() {

    // ViewBinding
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var contx: Context

    private val today = LocalDate.now()

    private var isFrontVisible = true // Stato per sapere quale lato è visibile
    private lateinit var cardFront : CardView
    private lateinit var cardBack : CardView

    private lateinit var weekCalendarView: ComposeView

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contx = requireContext()
        databaseHelper = DatabaseHelperSingleton.getInstance(contx)
        viewModel = ViewModelProvider(requireActivity()).get(WorkoutViewModel::class.java)
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

        contx = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    WeekCalendar(list)
                }
            }
        }

        binding.workoutCard.setOnClickListener {
            val intent = Intent(contx, ManageWorkoutActivity::class.java)
            startActivity(intent)
        }

        binding.runningCard.setOnClickListener {
            val intent = Intent(contx, MapsActivity::class.java)
            startActivity(intent)
        }

        binding.cardContainer.setOnClickListener { flipCard() }
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