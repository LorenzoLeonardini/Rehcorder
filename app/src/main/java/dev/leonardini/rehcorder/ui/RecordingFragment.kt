package dev.leonardini.rehcorder.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.Utils
import dev.leonardini.rehcorder.databinding.FragmentRecordingBinding

class RecordingFragment : Fragment(), Runnable {

    private var _binding: FragmentRecordingBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var animation: AnimatedVectorDrawableCompat
    private var isRecording = false
    private var isAnimationPaused = false
    private var isTimerPaused = false
    private var startTimestamp: Long = 0
    private var stopTimestamp: Long = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRecordingBinding.inflate(inflater, container, false)

        // Pulsating animation
        animation =
            AnimatedVectorDrawableCompat.create(requireContext(), R.drawable.recording_animation)!!
        animation.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                if (!isAnimationPaused) {
                    animation.start()
                }
            }
        })
        binding.animation.setBackgroundDrawable(animation)

        // Recover state
        stopTimestamp = savedInstanceState?.getLong("stopTimestamp", -1) ?: -1
        isRecording = savedInstanceState?.getBoolean("isRecording", false) ?: true
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

    override fun onStart() {
        super.onStart()

        if (isRecording) {
            isAnimationPaused = false
            animation.start()
            if (isTimerPaused) {
                isTimerPaused = false
                binding.recordingText.post(this)
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        binding.animation.visibility = View.GONE
        stopTimestamp = System.currentTimeMillis() / 1000
    }

    // Runnable implementation for restarting the pulsating animation
    override fun run() {
        val runningFor =
            (if (stopTimestamp > 0) stopTimestamp else System.currentTimeMillis() / 1000) - startTimestamp
        _binding?.recordingText?.text =
            Utils.secondsToTimeString(runningFor)
        if (isRecording && !isAnimationPaused)
            _binding?.recordingText?.postDelayed(this, 1000)
        else
            isTimerPaused = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean("isRecording", isRecording)
        outState.putLong("startTimestamp", startTimestamp)
        outState.putLong("stopTimestamp", stopTimestamp)
    }

    override fun onDestroyView() {
        animation.clearAnimationCallbacks()
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        isAnimationPaused = true
    }
}