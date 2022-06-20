package dev.leonardini.rehcorder.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.leonardini.rehcorder.R

class SongNameDialogFragment(
    private val positiveListener: (name: String) -> Unit
) : AppCompatDialogFragment() {

    private lateinit var materialDialog: Dialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v: View = LayoutInflater.from(activity).inflate(R.layout.dialog_rename, null)
        v.findViewById<TextInputLayout>(R.id.text_field).setHint(R.string.s_new_song_name)
        v.findViewById<TextInputEditText>(R.id.text_field_input)
            .doOnTextChanged { text, _, _, _ ->
                (materialDialog as androidx.appcompat.app.AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    text!!.isNotBlank()
            }

        isCancelable = false
        materialDialog = MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.s_new_title)
            .setPositiveButton(R.string.s_new_add) { _, _ ->
                positiveListener(
                    v.findViewById<TextInputEditText>(R.id.text_field_input).text.toString().trim()
                )
            }
            .setView(v)
            .create()
        materialDialog.show()
        (materialDialog as androidx.appcompat.app.AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
            false

        return materialDialog
    }
}