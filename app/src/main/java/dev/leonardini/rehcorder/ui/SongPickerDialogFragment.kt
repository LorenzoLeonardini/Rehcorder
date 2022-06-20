package dev.leonardini.rehcorder.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Song

class SongPickerDialogFragment(
    private val options: List<Song>,
    private val positiveListener: ((uid: Long) -> Unit)
) : AppCompatDialogFragment(), View.OnClickListener {

    private lateinit var materialDialog: Dialog
    private lateinit var textInputLayout: TextInputLayout
    private lateinit var empty: TextInputLayout
    private lateinit var autoCompleteTextView: MaterialAutoCompleteTextView
    private lateinit var addButton: MaterialButton
    private var songId: Long = -1

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v: View = LayoutInflater.from(activity).inflate(R.layout.dialog_pick_song, null)

        textInputLayout = v.findViewById(R.id.text_field)
        empty = v.findViewById(R.id.empty)
        autoCompleteTextView = v.findViewById(R.id.autocomplete)
        addButton = v.findViewById(R.id.add_song)
        addButton.setOnClickListener(this)

        autoCompleteTextView.setAdapter(ArrayAdapter(context!!, R.layout.autocomplete_list_item, options))
        autoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                songId = (parent.getItemAtPosition(position) as Song).uid
                (materialDialog as androidx.appcompat.app.AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    true
            }

        if (autoCompleteTextView.adapter.count == 0) {
            empty.visibility = View.VISIBLE
            textInputLayout.visibility = View.GONE
        } else {
            empty.visibility = View.GONE
            textInputLayout.visibility = View.VISIBLE
        }

        isCancelable = false
        materialDialog = MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.s_picker_title)
            .setPositiveButton(R.string.save) { _, _ ->
                positiveListener(songId)
            }
            .setView(v)
            .create()
        materialDialog.show()
        (materialDialog as androidx.appcompat.app.AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
            false

        return materialDialog
    }

    override fun onClick(v: View?) {
        SongNameDialogFragment { name ->
            Thread {
                val song = Song(name)
                song.uid = Database.getInstance(context!!).songDao().insert(song)
                autoCompleteTextView.post {
                    (autoCompleteTextView.adapter as ArrayAdapter<Song>).let { adapter ->
                        adapter.add(song)
                        adapter.sort { o1, o2 -> o1.name.compareTo(o2.name) }
                        adapter.notifyDataSetChanged()
                    }
                    empty.visibility = View.GONE
                    textInputLayout.visibility = View.VISIBLE
                }
            }.start()
        }.show(parentFragmentManager, "NameDialog")
    }
}