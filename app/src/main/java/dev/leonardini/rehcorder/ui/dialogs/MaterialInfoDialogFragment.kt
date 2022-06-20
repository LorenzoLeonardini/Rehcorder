package dev.leonardini.rehcorder.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialInfoDialogFragment(
    private val title: Int,
    private val message: Int,
) : AppCompatDialogFragment(), DialogInterface.OnClickListener {

    constructor() : this(-1, -1)

    private var _title: Int = -1
    private var _message: Int = -1

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, this)
            .setCancelable(false)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("title", _title)
        outState.putInt("message", _message)
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        val bundle = Bundle()
        bundle.putInt("which", which)
        activity!!.supportFragmentManager.setFragmentResult(tag ?: this::class.simpleName!!, bundle)
    }

}