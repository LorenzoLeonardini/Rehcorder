package dev.leonardini.rehcorder.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dev.leonardini.rehcorder.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
