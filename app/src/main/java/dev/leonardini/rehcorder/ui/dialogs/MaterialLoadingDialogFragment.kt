package dev.leonardini.rehcorder.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.leonardini.rehcorder.R

/**
 * Basic material dialog to provide loading feedback to the user. Must be closed programmatically
 */
class MaterialLoadingDialogFragment : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v: View = LayoutInflater.from(activity).inflate(R.layout.dialog_loading, null)

        isCancelable = false
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.loading)
            .setView(v)
            .create()
    }
}