package com.example.myorangefit
import android.content.Context

object DatabaseHelperSingleton {
    private var instance: DatabaseHelper? = null

    fun getInstance(context: Context): DatabaseHelper {
        if (instance == null) {
            instance = DatabaseHelper(context.applicationContext)
        }
        return instance!!
    }
}
