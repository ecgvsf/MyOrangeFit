package com.example.myorangefit.async

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myorangefit.database.DatabaseHelper

class WorkoutViewModelFactory(private val databaseHelper: DatabaseHelper) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(databaseHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}