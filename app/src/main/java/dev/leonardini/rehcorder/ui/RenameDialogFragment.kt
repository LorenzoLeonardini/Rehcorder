package dev.leonardini.rehcorder.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.leonardini.rehcorder.R

class RenameDialogFragment(
    private val currentName: String?,
    private val hintString: Int,
    private val positiveListener: (name: String) -> Unit
) : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v: View = LayoutInflater.from(activity).inflate(R.layout.dialog_rename, null)
        v.findViewById<TextInputEditText>(R.id.text_field_input).setText(currentName ?: "")
        v.findViewById<TextInputLayout>(R.id.text_field).setHint(hintString)

        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.rename)
            .setPositiveButton(R.string.save) { _, _ ->
                positiveListener(
                    v.findViewById<TextInputEditText>(R.id.text_field_input).text.toString().trim()
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .setView(v)
            .create()
    }
}