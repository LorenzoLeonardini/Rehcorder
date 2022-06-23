package dev.leonardini.rehcorder.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import dev.leonardini.rehcorder.MainActivity
import dev.leonardini.rehcorder.ProcessActivity
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.databinding.FragmentRecordingBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class RecordingFragment : Fragment(), Runnable {

    private var _binding: FragmentRecordingBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var animation: AnimatedVectorDrawableCompat
    private var isRecording = false
    private var startTimestamp: Long = 0
    private var stopTimestamp: Long = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRecordingBinding.inflate(inflater, container, false)

        animation =
            AnimatedVectorDrawableCompat.create(requireContext(), R.drawable.recording_animation)!!
        animation.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                _binding?.animation?.post {
                    animation.start()
                }
            }
        })
        binding.animation.setBackgroundDrawable(animation)
        animation.start()

        stopTimestamp = savedInstanceState?.getLong("stopTimestamp", -1) ?: -1

        isRecording = (requireActivity() as MainActivity).recording
        if (stopTimestamp > 0 || isRecording) {
            startTimestamp =
                savedInstanceState?.getLong("startTimestamp") ?: arguments?.getLong("timestamp")
                        ?: (System.currentTimeMillis() / 1000)
            binding.recordingText.post(this)
        }
        if (isRecording)
            binding.animation.visibility = View.VISIBLE

        return binding.root
    }

    fun stopRecording() {
        isRecording = false
        binding.animation.visibility = View.GONE
        stopTimestamp = System.currentTimeMillis() / 1000
    }

    override fun run() {
        val runningFor =
            (if (stopTimestamp > 0) stopTimestamp else System.currentTimeMillis() / 1000) - startTimestamp
        _binding?.recordingText?.text =
            ProcessActivity.secondsToTimeString(runningFor)
        if (isRecording)
            _binding?.recordingText?.postDelayed(this, 1000)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putLong("startTimestamp", startTimestamp)
        outState.putLong("stopTimestamp", stopTimestamp)
    }

    override fun onDestroyView() {
        animation.clearAnimationCallbacks()
        super.onDestroyView()
        _binding = null
    }
}