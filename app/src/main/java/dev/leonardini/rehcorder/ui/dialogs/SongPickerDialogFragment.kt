package dev.leonardini.rehcorder.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Song
import dev.leonardini.rehcorder.ui.RehearsalsFragment

class SongPickerDialogFragment(
    private val songs: ArrayList<Song>,
) : AppCompatDialogFragment(), DialogInterface.OnClickListener {

    constructor() : this(ArrayList<Song>())

    private lateinit var _songs: ArrayList<Song>
    private lateinit var _materialDialog: Dialog
    private lateinit var _adapter: ArrayAdapter<Song>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        songs.add(0, Song(context!!.getString(R.string.s_picker_new_song)))
        _songs = savedInstanceState?.getParcelableArrayList<Song>("songs") ?: songs
        _adapter = ArrayAdapter(
            context!!,
            R.layout.autocomplete_list_item,
            _songs
        )

        isCancelable = false
        _materialDialog = MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.s_picker_title)
            .setAdapter(_adapter, this)
            .create()
        _materialDialog.show()
        (_materialDialog as androidx.appcompat.app.AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
            false

        return _materialDialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("songs", _songs)
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        if (which == 0) {
            SongNameDialogFragment().show(parentFragmentManager, "NewSongNameDialog")
        } else {
            val bundle = Bundle()
            bundle.putLong("id", _adapter.getItem(which)!!.uid)
            activity!!.supportFragmentManager.setFragmentResult(
                tag ?: this::class.simpleName!!,
                bundle
            )
        }
    }
}