package dev.leonardini.alarm

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialDialogFragment(
        private val title :Int,
        private val message :Int,
        private val positiveListener :DialogInterface.OnClickListener?,
        private val negativeListener :DialogInterface.OnClickListener?
    ) : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, positiveListener)
            .setNegativeButton(android.R.string.cancel, negativeListener)
            .setCancelable(false)
            .create()
    }

}