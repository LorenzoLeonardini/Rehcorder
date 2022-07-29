package dev.leonardini.rehcorder.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.DoNotInline
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.preference.DialogPreference
import androidx.preference.DialogPreference.TargetFragment
import androidx.preference.ListPreferenceDialogFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Code stolen from PreferenceDialogFragmentCompat, had to replace the old AlertDialog.Builder with MaterialAlertDialogBuilder
 */
@SuppressLint("RestrictedApi")
class ListPreferenceDialogFragment : ListPreferenceDialogFragmentCompat() {

    companion object {
        private const val SAVE_STATE_TITLE = "PreferenceDialogFragment.title"
        private const val SAVE_STATE_POSITIVE_TEXT = "PreferenceDialogFragment.positiveText"
        private const val SAVE_STATE_NEGATIVE_TEXT = "PreferenceDialogFragment.negativeText"
        private const val SAVE_STATE_MESSAGE = "PreferenceDialogFragment.message"
        private const val SAVE_STATE_LAYOUT = "PreferenceDialogFragment.layout"
        private const val SAVE_STATE_ICON = "PreferenceDialogFragment.icon"

        fun newInstance(key: String?): ListPreferenceDialogFragment {
            val fragment = ListPreferenceDialogFragment()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }

    private lateinit var mPreference: DialogPreference

    private var mDialogTitle: CharSequence? = null
    private var mPositiveButtonText: CharSequence? = null
    private var mNegativeButtonText: CharSequence? = null
    private var mDialogMessage: CharSequence? = null

    @LayoutRes
    private var mDialogLayoutRes = 0

    private var mDialogIcon: BitmapDrawable? = null

    /** Which button was clicked.  */
    private var mWhichButtonClicked = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rawFragment = targetFragment
        check(rawFragment is TargetFragment) {
            "Target fragment must implement TargetFragment" +
                    " interface"
        }
        val fragment = rawFragment as TargetFragment
        val key = requireArguments().getString(ARG_KEY)
        if (savedInstanceState == null) {
            mPreference = fragment.findPreference(key!!)!!
            mDialogTitle = mPreference.dialogTitle
            mPositiveButtonText = mPreference.positiveButtonText
            mNegativeButtonText = mPreference.negativeButtonText
            mDialogMessage = mPreference.dialogMessage
            mDialogLayoutRes = mPreference.dialogLayoutResource
            val icon = mPreference.dialogIcon
            mDialogIcon = if (icon == null || icon is BitmapDrawable) {
                icon as BitmapDrawable?
            } else {
                val bitmap = Bitmap.createBitmap(
                    icon.intrinsicWidth,
                    icon.intrinsicHeight, Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                icon.setBounds(0, 0, canvas.width, canvas.height)
                icon.draw(canvas)
                BitmapDrawable(resources, bitmap)
            }
        } else {
            mDialogTitle = savedInstanceState.getCharSequence(SAVE_STATE_TITLE)
            mPositiveButtonText = savedInstanceState.getCharSequence(SAVE_STATE_POSITIVE_TEXT)
            mNegativeButtonText = savedInstanceState.getCharSequence(SAVE_STATE_NEGATIVE_TEXT)
            mDialogMessage = savedInstanceState.getCharSequence(SAVE_STATE_MESSAGE)
            mDialogLayoutRes = savedInstanceState.getInt(SAVE_STATE_LAYOUT, 0)
            val bitmap = savedInstanceState.getParcelable<Bitmap>(SAVE_STATE_ICON)
            if (bitmap != null) {
                mDialogIcon = BitmapDrawable(resources, bitmap)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(mDialogTitle)
            .setIcon(mDialogIcon)
            .setPositiveButton(mPositiveButtonText, this)
            .setNegativeButton(mNegativeButtonText, this)
        val contentView = onCreateDialogView(requireContext())
        if (contentView != null) {
            onBindDialogView(contentView)
            builder.setView(contentView)
        } else {
            builder.setMessage(mDialogMessage)
        }
        onPrepareDialogBuilder(builder)

        // Create the dialog
        val dialog: Dialog = builder.create()
        if (needInputMethod()) {
            requestInputMethod(dialog)
        }
        return dialog
    }

    private fun requestInputMethod(dialog: Dialog) {
        val window = dialog.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Api30Impl.showIme(window!!)
        } else {
            scheduleShowSoftInput()
        }
    }


    @RequiresApi(Build.VERSION_CODES.R)
    private object Api30Impl {
        /**
         * Shows the IME on demand for the given [Window].
         */
        @DoNotInline
        fun showIme(dialogWindow: Window) {
            dialogWindow.decorView.windowInsetsController!!.show(WindowInsets.Type.ime())
        }
    }

}