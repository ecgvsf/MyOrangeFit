package com.example.myorangefit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class WeightExerciseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityManager.add(this)
        setContentView(R.layout.activity_weight_exercise)
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
    }
}