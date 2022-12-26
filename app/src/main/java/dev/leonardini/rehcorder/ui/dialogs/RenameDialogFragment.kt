package dev.leonardini.rehcorder.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.leonardini.rehcorder.R

/**
 * Material dialog providing a text input to rename a rehearsal
 */
class RenameDialogFragment(
    private val itemId: Long,
    private val currentName: String?,
    private val hintString: Int
) : AppCompatDialogFragment(), DialogInterface.OnClickListener {

    private lateinit var v: View
    private var _itemId: Long = -1
    private var _currentName: String? = null
    private var _hintString: Int = -1

    constructor() : this(-1, null, -1)
    constructor(currentName: String?, hintString :Int) : this(-1, currentName, hintString)

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _itemId = savedInstanceState?.getLong("itemId") ?: itemId
        _currentName = savedInstanceState?.getString("currentName") ?: currentName
        _hintString = savedInstanceState?.getInt("hintString") ?: hintString

        v = LayoutInflater.from(activity).inflate(R.layout.dialog_rename, null)
        v.findViewById<TextInputEditText>(R.id.text_field_input).setText(_currentName ?: "")
        v.findViewById<TextInputLayout>(R.id.text_field).setHint(_hintString)

        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.rename)
            .setPositiveButton(R.string.save, this)
            .setNegativeButton(R.string.cancel, null)
            .setView(v)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("itemId", _itemId)
        outState.putString(
            "currentName",
            v.findViewById<TextInputEditText>(R.id.text_field_input).text.toString().trim()
        )
        outState.putInt("hintString", _hintString)
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        val bundle = Bundle()
        bundle.putLong("id", _itemId)
        bundle.putString(
            "name",
            v.findViewById<TextInputEditText>(R.id.text_field_input).text.toString().trim()
        )
        requireActivity().supportFragmentManager.setFragmentResult(
            tag ?: this::class.simpleName!!,
            bundle
        )
    }
}