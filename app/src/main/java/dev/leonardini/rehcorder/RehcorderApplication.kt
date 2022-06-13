package dev.leonardini.rehcorder

import android.app.Application
import com.google.android.material.color.DynamicColors

class RehcorderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}