package com.robertlevonyan.demo.camerax.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraX
import androidx.camera.core.FlashMode
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.core.view.ViewCompat
import androidx.navigation.Navigation
import com.robertlevonyan.demo.camerax.R
import com.robertlevonyan.demo.camerax.databinding.FragmentVideoBinding
import com.robertlevonyan.demo.camerax.utils.SharedPrefsManager
import com.robertlevonyan.demo.camerax.utils.SwipeGestureDetector
import com.robertlevonyan.demo.camerax.utils.toggleButton
import kotlin.properties.Delegates

class VideoFragment : BaseFragment<FragmentVideoBinding>(R.layout.fragment_video) {
    companion object {
        private const val TAG = "VideoFragment"
        const val KEY_FLASH = "sPrefFlashVideo"
        const val KEY_GRID = "sPrefGridVideo"
    }

    private lateinit var displayManager: DisplayManager
    private lateinit var prefs: SharedPrefsManager
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture

    private var displayId = -1
    private var lensFacing = CameraX.LensFacing.BACK
    private var flashMode by Delegates.observable(FlashMode.OFF.ordinal) { _, _, new ->
        binding.buttonFlash.setImageResource(
            when (new) {
                FlashMode.ON.ordinal -> R.drawable.ic_flash_on
                else -> R.drawable.ic_flash_off
            }
        )
    }
    private var hasGrid = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@VideoFragment.displayId) {
                preview.setTargetRotation(view.display.rotation)
                imageCapture.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = SharedPrefsManager.newInstance(requireContext())
        flashMode = prefs.getInt(KEY_FLASH, FlashMode.OFF.ordinal)
        hasGrid = prefs.getBoolean(KEY_GRID, false)
        initViews()

        displayManager = requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        binding.fragment = this
        binding.viewFinder.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View) =
                displayManager.registerDisplayListener(displayListener, null)

            override fun onViewAttachedToWindow(v: View) = displayManager.unregisterDisplayListener(displayListener)
        })

        val swipeGestures = SwipeGestureDetector().apply {
            setSwipeCallback(left = { Navigation.findNavController(view).navigate(R.id.action_video_to_camera) })
        }
        val gestureDetectorCompat = GestureDetector(requireContext(), swipeGestures)
        view.setOnTouchListener { _, motionEvent ->
            if (gestureDetectorCompat.onTouchEvent(motionEvent)) return@setOnTouchListener false
            return@setOnTouchListener true
        }
    }

    private fun initViews() {
        binding.buttonGrid.setImageResource(if (hasGrid) R.drawable.ic_grid_on else R.drawable.ic_grid_off)
        binding.groupGridLines.visibility = if (hasGrid) View.VISIBLE else View.GONE

        adjustInsets()
    }

    private fun adjustInsets() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            ViewCompat.requestApplyInsets(binding.fabRecordVideo)
            ViewCompat.requestApplyInsets(binding.buttonFlash)
            ViewCompat.setOnApplyWindowInsetsListener(binding.fabRecordVideo) { v, insets ->
                val params = v.layoutParams as ViewGroup.MarginLayoutParams
                params.bottomMargin = insets.systemWindowInsetBottom
                v.layoutParams = params
                insets.consumeSystemWindowInsets()
            }
            ViewCompat.setOnApplyWindowInsetsListener(binding.buttonFlash) { v, insets ->
                val params = v.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = insets.systemWindowInsetTop
                v.layoutParams = params
                insets.consumeSystemWindowInsets()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    fun toggleCamera() = binding.buttonSwitchCamera.toggleButton(
        lensFacing == CameraX.LensFacing.BACK, 180f,
        R.drawable.ic_outline_camera_rear, R.drawable.ic_outline_camera_front
    ) {
        lensFacing = if (it) CameraX.LensFacing.BACK else CameraX.LensFacing.FRONT

        CameraX.getCameraWithLensFacing(lensFacing)
        recreateCamera()
    }

    private fun recreateCamera() {

    }

    fun openPreview() {
        if (!outputDirectory.listFiles().isNullOrEmpty())
            view?.let { Navigation.findNavController(it).navigate(R.id.action_video_to_preview) }
    }

    fun toggleGrid() =
        binding.buttonGrid.toggleButton(hasGrid, 180f, R.drawable.ic_grid_off, R.drawable.ic_grid_on) { flag ->
            hasGrid = flag
            prefs.putBoolean(KEY_GRID, flag)
            binding.groupGridLines.visibility = if (flag) View.VISIBLE else View.GONE
        }

    fun toggleFlash() {

    }

    override fun onPermissionGranted() {
        val viewFinder = binding.viewFinder

        viewFinder.post {
            displayId = binding.viewFinder.display.displayId
            recreateCamera()
        }
    }

    override fun onBackPressed() = requireActivity().finish()

}
