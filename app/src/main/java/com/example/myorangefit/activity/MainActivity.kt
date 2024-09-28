package com.example.myorangefit.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myorangefit.R
import com.example.myorangefit.async.WorkoutViewModel
import com.example.myorangefit.async.WorkoutViewModelFactory
import com.example.myorangefit.database.DatabaseHelper
import com.example.myorangefit.database.DatabaseHelperSingleton
import com.example.myorangefit.databinding.ActivityMainBinding
import com.example.myorangefit.fragment.CalendarFragment
import com.example.myorangefit.fragment.HomeFragment
import com.example.myorangefit.fragment.StatisticFragment
import com.example.myorangefit.fragment.UserFragment
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.*
import java.time.LocalDate

class MainActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var viewModel: WorkoutViewModel

    private lateinit var binding: ActivityMainBinding

    private val today = LocalDate.now()

    private val iconSize = 128

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        ActivityManager.add(this)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        databaseHelper = DatabaseHelperSingleton.getInstance(this)
        val factory = WorkoutViewModelFactory(databaseHelper)
        viewModel = ViewModelProvider(this, factory).get(WorkoutViewModel::class.java)

        // Load initial data
        loadData()

        //------------------------------------------------------------------------------------

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val daysOfWeek = daysOfWeek()

        val bottomNavigationView = binding.bottomNavigation

        // Definisci i colori per lo stato selezionato e non selezionato
        val colorStateList = ContextCompat.getColorStateList(this, R.color.color_state_list)

        // Imposta i colori per le icone e per il testo degli item
        bottomNavigationView.itemIconTintList = colorStateList

        // Itera su tutti gli item del menu
        for (i in 0 until bottomNavigationView.menu.size()) {
            val menuItem = bottomNavigationView.menu.getItem(i)

            // Recupera la vista dell'item del menu
            val itemView = bottomNavigationView.findViewById<View>(menuItem.itemId) as ViewGroup

            // Infla il layout personalizzato
            val customView =
                LayoutInflater.from(this).inflate(R.layout.bottom_nav_item, itemView, false)

            // Imposta l'icona corretta
            val icon = customView.findViewById<ImageView>(R.id.icon)

            if (i != 2) {
                icon.setImageDrawable(menuItem.icon)
                icon.drawable.setTintList(colorStateList)
                icon.layoutParams.width = iconSize
                icon.layoutParams.height = iconSize


            } else {
                icon.layoutParams.width = iconSize * 1.5.toInt()
                icon.layoutParams.height = iconSize * 1.5.toInt()
            }

            // Rimuovi le viste precedenti e aggiungi la nuova vista
            itemView.removeAllViews()
            itemView.addView(customView)
        }

        bottomNavigationView.menu.getItem(0).icon?.setTint(getResources().getColor(R.color.primary))

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->

            for (i in 0 until bottomNavigationView.menu.size()) {
                val menuItem = bottomNavigationView.menu.getItem(i)
                if (i != 2)
                    menuItem.icon?.setTint(Color.WHITE)
            }

            item.icon?.setTint(getResources().getColor(R.color.primary))
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.navigation_calendar -> {
                    loadFragment(CalendarFragment())
                    true
                }
                R.id.navigation_statistics -> {
                    loadFragment(StatisticFragment())
                    true
                }
                R.id.navigation_user -> {
                    loadFragment(UserFragment())
                    true
                }
                else -> false
            }
        }

        bottomNavigationView.menu.forEach { menuItem ->
            val itemView = bottomNavigationView.findViewById<View>(menuItem.itemId)

            // Imposta un listener di tocco per ridurre l'area di tocco
            itemView.setOnTouchListener { v, event ->
                // Ottieni le dimensioni del pulsante
                val height = v.height
                val touchY = event.y

                // Se l'evento è al di sopra della metà del pulsante, ignora il tocco
                if (touchY < (height / 3f)) {
                    // Ignora il tocco nella parte superiore
                    return@setOnTouchListener true
                } else {
                    // Gestisci normalmente l'evento di tocco nella metà inferiore
                    return@setOnTouchListener false
                }
            }
        }


        binding.fab.setOnClickListener{
            var date = today.toString()
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

            if (fragment is CalendarFragment) {
                viewModel.selectedData.observe(this) { data ->
                    date = data.toString()
                }
            }

            val intent = Intent(this, BodyPartActivity::class.java)
            intent.putExtra("selectedDate", date)
            intent.putExtra("flag", 1)
            startActivity(intent)
        }

        loadFragment(HomeFragment())
    }

    private fun loadData() {
        val today = LocalDate.now()
        viewModel.updateData(today)
        viewModel.loadWorkoutsForDate(today)
        viewModel.loadAllWorkouts()
        viewModel.getStreak()
        viewModel.getWeekWorkout()
        viewModel.getWeekDates(today)
    }

    private fun loadFragment(fragment: Fragment) {
        Log.d("ciao","$fragment")
        if (today != null) {
            val bundle = Bundle()
            bundle.putSerializable("today", today)
            fragment.arguments = bundle
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }


    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
    }
}