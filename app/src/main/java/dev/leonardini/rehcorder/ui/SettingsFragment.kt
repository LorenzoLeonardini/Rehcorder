package dev.leonardini.rehcorder.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.preference.*
import dev.leonardini.rehcorder.R

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val UNPROCESSED_MICROPHONE = "unprocessed_microphone"
        const val SAMPLE_RATE = "sample_rate"
        const val DELETE_RECORDING = "delete_recording"
        private const val DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG"
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    @SuppressLint("RestrictedApi")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        var handled = false
        if (callbackFragment is OnPreferenceDisplayDialogCallback) {
            handled =
                (callbackFragment as OnPreferenceDisplayDialogCallback?)!!.onPreferenceDisplayDialog(
                    this,
                    preference
                )
        }
        //  If the callback fragment doesn't handle OnPreferenceDisplayDialogCallback, looks up
        //  its parent fragment in the hierarchy that implements the callback until the first
        //  one that returns true
        var callbackFragment: Fragment? = this
        while (!handled && callbackFragment != null) {
            if (callbackFragment is OnPreferenceDisplayDialogCallback) {
                handled = (callbackFragment as OnPreferenceDisplayDialogCallback)
                    .onPreferenceDisplayDialog(this, preference)
            }
            callbackFragment = callbackFragment.parentFragment
        }
        if (!handled && context is OnPreferenceDisplayDialogCallback) {
            handled = (context as OnPreferenceDisplayDialogCallback?)!!
                .onPreferenceDisplayDialog(this, preference)
        }
        // Check the Activity as well in case getContext was overridden to return something other
        // than the Activity.
        if (!handled && activity is OnPreferenceDisplayDialogCallback) {
            handled = (activity as OnPreferenceDisplayDialogCallback?)!!
                .onPreferenceDisplayDialog(this, preference)
        }
        if (handled) {
            return
        }

        // check if dialog is already showing
        if (parentFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return
        }
        val f: DialogFragment = when (preference) {
            is EditTextPreference -> {
                EditTextPreferenceDialogFragmentCompat.newInstance(preference.getKey())
            }
            is ListPreference -> {
                ListPreferenceDialogFragment.newInstance(preference.getKey())
            }
            is MultiSelectListPreference -> {
                MultiSelectListPreferenceDialogFragmentCompat.newInstance(preference.getKey())
            }
            else -> {
                throw IllegalArgumentException(
                    "Cannot display dialog for an unknown Preference type: "
                            + preference.javaClass.simpleName
                            + ". Make sure to implement onPreferenceDisplayDialog() to handle "
                            + "displaying a custom dialog for this Preference."
                )
            }
        }
        f.setTargetFragment(this, 0)
        f.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
    }
}
