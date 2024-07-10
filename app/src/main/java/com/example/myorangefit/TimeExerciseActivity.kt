package com.example.myorangefit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class TimeExerciseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityManager.add(this)
        setContentView(R.layout.activity_time_exercise)
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
    }
}