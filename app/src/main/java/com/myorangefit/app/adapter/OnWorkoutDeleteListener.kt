package com.myorangefit.app.adapter

import com.myorangefit.app.model.Workout

interface OnWorkoutDeleteListener {
    fun onDelete(workout: Workout)
}
