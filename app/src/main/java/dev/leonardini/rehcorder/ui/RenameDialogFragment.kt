package dev.leonardini.rehcorder.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.leonardini.rehcorder.R

class RenameDialogFragment(
    private val currentName: String?,
    private val positiveListener: (name: String?) -> Unit
) : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v: View = LayoutInflater.from(activity).inflate(R.layout.dialog_rename, null)
        v.findViewById<TextInputEditText>(R.id.text_field_input).setText(currentName ?: "")

        isCancelable = false
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle("Rename")
            .setPositiveButton("Save", if (positiveListener != null) { _, _ ->
                positiveListener(v.findViewById<TextInputEditText>(R.id.text_field_input).text.toString())
            } else null)
            .setNegativeButton("Cancel", null)
            .setView(v)
            .create()
    }
}