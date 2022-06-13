package dev.leonardini.rehcorder

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.leonardini.rehcorder.R

class MaterialLoadingDialogFragment : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v: View = LayoutInflater.from(activity).inflate(R.layout.dialog_loading, null)

        isCancelable = false
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle("Loading")
            .setView(v)
            .create()
    }
}