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
) : AppCompatDialogFragment(), View.OnClickListener, DialogInterface.OnClickListener {

    constructor() : this(ArrayList<Song>())

    private lateinit var _songs: ArrayList<Song>
    private lateinit var _materialDialog: Dialog
    private lateinit var _textInputLayout: TextInputLayout
    private lateinit var _empty: TextInputLayout
    private lateinit var _autoCompleteTextView: MaterialAutoCompleteTextView
    private lateinit var _addButton: MaterialButton
    private var _songId: Long = -1L

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v: View = LayoutInflater.from(activity).inflate(R.layout.dialog_pick_song, null)

        _textInputLayout = v.findViewById(R.id.text_field)
        _empty = v.findViewById(R.id.empty)
        _autoCompleteTextView = v.findViewById(R.id.autocomplete)
        _addButton = v.findViewById(R.id.add_song)
        _addButton.setOnClickListener(this)

        _songs = savedInstanceState?.getParcelableArrayList<Song>("songs") ?: songs
        _autoCompleteTextView.setAdapter(
            ArrayAdapter(
                context!!,
                R.layout.autocomplete_list_item,
                _songs
            )
        )
        _autoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                _songId = (parent.getItemAtPosition(position) as Song).uid
                (_materialDialog as androidx.appcompat.app.AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    true
            }
        _songId = savedInstanceState?.getLong("songId") ?: -1L
        if (_songId != -1L) {
            val songName = _songs.filter { song -> song.uid == _songId }[0].name
            _autoCompleteTextView.setText(songName)
        }

        if (_autoCompleteTextView.adapter.count == 0) {
            _empty.visibility = View.VISIBLE
            _textInputLayout.visibility = View.GONE
        } else {
            _empty.visibility = View.GONE
            _textInputLayout.visibility = View.VISIBLE
        }

        isCancelable = false
        _materialDialog = MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.s_picker_title)
            .setPositiveButton(R.string.save, this)
            .setView(v)
            .create()
        _materialDialog.show()
        (_materialDialog as androidx.appcompat.app.AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
            false

        return _materialDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        activity!!.supportFragmentManager.setFragmentResultListener(
            "NameDialog",
            activity!!
        ) { _, bundle ->
            val name = bundle.getString("name")
            Thread {
                val song = Song(name!!)
                song.uid = Database.getInstance(context!!).songDao().insert(song)
                _autoCompleteTextView.post {
                    (_autoCompleteTextView.adapter as ArrayAdapter<Song>).let { adapter ->
                        adapter.add(song)
                        adapter.sort { o1, o2 -> o1.name.compareTo(o2.name) }
                        adapter.notifyDataSetChanged()
                    }
                    _empty.visibility = View.GONE
                    _textInputLayout.visibility = View.VISIBLE
                }
            }.start()
        }
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("songs", _songs)
        outState.putLong("songId", _songId)
    }

    override fun onClick(v: View?) {
        SongNameDialogFragment().show(parentFragmentManager, "NameDialog")
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        val bundle = Bundle()
        bundle.putLong("id", _songId)
        activity!!.supportFragmentManager.setFragmentResult(tag ?: this::class.simpleName!!, bundle)
    }
}