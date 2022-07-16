package dev.leonardini.rehcorder.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dev.leonardini.rehcorder.R

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val UNPROCESSED_MICROPHONE = "unprocessed_microphone"
        const val SAMPLE_RATE = "sample_rate"
        const val DELETE_RECORDING = "delete_recording"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
