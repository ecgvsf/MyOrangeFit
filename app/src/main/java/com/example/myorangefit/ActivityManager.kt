package com.example.myorangefit

import android.app.Activity

object ActivityManager {
    private val activities = mutableListOf<Activity>()

    fun add(activity: Activity) {
        activities.add(activity)
    }

    fun remove(activity: Activity) {
        activities.remove(activity)
    }

    fun finishAll() {
        for (activity in activities) {
            if (!activity.isFinishing) {
                activity.finish()
            }
        }
        activities.clear()
    }

    fun finish(activity: Activity){
        val foundActivity = activities.find { it == activity }
        foundActivity?.finish()
    }
}
