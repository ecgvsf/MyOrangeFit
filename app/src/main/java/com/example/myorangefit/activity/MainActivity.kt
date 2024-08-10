package com.example.myorangefit.activity

import android.animation.AnimatorSet
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActionBar.LayoutParams
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myorangefit.MapsActivity
import com.example.myorangefit.R
import com.example.myorangefit.adapter.ExerciseCalendarAdapter
import com.example.myorangefit.database.DatabaseHelper
import com.example.myorangefit.database.DatabaseHelperSingleton
import com.example.myorangefit.databinding.ActivityMainBinding
import com.example.myorangefit.databinding.CalendarDayBinding
import com.example.myorangefit.fragment.CalendarFragment
import com.example.myorangefit.fragment.HomeFragment
import com.example.myorangefit.fragment.StatisticFragment
import com.example.myorangefit.fragment.UserFragment
import com.example.myorangefit.model.Workout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.internal.ViewUtils.dpToPx
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var exerciseAdapter: ExerciseCalendarAdapter

    private lateinit var binding: ActivityMainBinding

    private val selectedDates = mutableSetOf<LocalDate>()
    private var selectedDate: LocalDate? = null
    private var isWeekMode = false
    private val today = LocalDate.now()

    private val iconSize = 128

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityManager.add(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val daysOfWeek = daysOfWeek()

        databaseHelper = DatabaseHelperSingleton.getInstance(this)
        loadFragment(HomeFragment())


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