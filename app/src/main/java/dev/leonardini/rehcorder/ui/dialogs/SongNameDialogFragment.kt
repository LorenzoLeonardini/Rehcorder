package dev.leonardini.rehcorder.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.leonardini.rehcorder.R

class SongNameDialogFragment : AppCompatDialogFragment(), DialogInterface.OnClickListener {

    private lateinit var v: View
    private var _currentName: String? = null
    private lateinit var materialDialog: Dialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _currentName = savedInstanceState?.getString("name") ?: ""

        v = LayoutInflater.from(activity).inflate(R.layout.dialog_rename, null)
        v.findViewById<TextInputLayout>(R.id.text_field).setHint(R.string.s_new_song_name)
        v.findViewById<TextInputEditText>(R.id.text_field_input).setText(_currentName)
        v.findViewById<TextInputEditText>(R.id.text_field_input)
            .doOnTextChanged { text, _, _, _ ->
                (materialDialog as androidx.appcompat.app.AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    text!!.isNotBlank()
            }

        isCancelable = false
        materialDialog = MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.s_new_title)
            .setPositiveButton(R.string.s_new_add, this)
            .setNegativeButton(R.string.cancel, null)
            .setView(v)
            .create()
        materialDialog.show()
        (materialDialog as androidx.appcompat.app.AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
            false

        return materialDialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(
            "name",
            v.findViewById<TextInputEditText>(R.id.text_field_input).text.toString().trim()
        )
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        val bundle = Bundle()
        bundle.putString(
            "name",
            v.findViewById<TextInputEditText>(R.id.text_field_input).text.toString().trim()
        )
        activity!!.supportFragmentManager.setFragmentResult(tag ?: this::class.simpleName!!, bundle)
    }
}