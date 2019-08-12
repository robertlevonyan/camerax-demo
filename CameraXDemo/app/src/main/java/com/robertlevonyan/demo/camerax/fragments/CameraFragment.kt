package com.robertlevonyan.demo.camerax.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.GestureDetector
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.robertlevonyan.demo.camerax.R
import com.robertlevonyan.demo.camerax.databinding.FragmentCameraBinding
import com.robertlevonyan.demo.camerax.enums.CameraTimer
import com.robertlevonyan.demo.camerax.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.properties.Delegates

class CameraFragment : BaseFragment<FragmentCameraBinding>(R.layout.fragment_camera) {
    companion object {
        private const val TAG = "CameraFragment"
        const val KEY_FLASH = "sPrefFlashCamera"
        const val KEY_GRID = "sPrefGridCamera"
    }

    private lateinit var displayManager: DisplayManager
    private lateinit var prefs: SharedPrefsManager
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalyzer: ImageAnalysis

    private var displayId = -1
    private var lensFacing = CameraX.LensFacing.BACK
    private var flashMode by Delegates.observable(FlashMode.OFF.ordinal) { _, _, new ->
        binding.buttonFlash.setImageResource(
            when (new) {
                FlashMode.ON.ordinal -> R.drawable.ic_flash_on
                FlashMode.AUTO.ordinal -> R.drawable.ic_flash_auto
                else -> R.drawable.ic_flash_off
            }
        )
    }
    private var hasGrid = false
    private var selectedTimer = CameraTimer.OFF

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                preview.setTargetRotation(view.display.rotation)
                imageCapture.setTargetRotation(view.display.rotation)
                imageAnalyzer.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    @SuppressLint("ClickableViewAccessibility")
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
            setSwipeCallback(right = { Navigation.findNavController(view).navigate(R.id.action_camera_to_video) })
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
        binding.viewFinder.fitSystemWindows()
        binding.fabTakePicture.onWindowInsets { view, windowInsets ->
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                view.bottomMargin = windowInsets.systemWindowInsetBottom
            else view.endMargin = windowInsets.systemWindowInsetRight
        }
        binding.buttonFlash.onWindowInsets { view, windowInsets ->
            view.topMargin = windowInsets.systemWindowInsetTop
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
        CameraX.unbindAll()
        startCamera()
    }

    fun openPreview() {
        if (!outputDirectory.listFiles().isNullOrEmpty())
            view?.let { Navigation.findNavController(it).navigate(R.id.action_camera_to_preview) }
    }

    fun selectTimer() = binding.layoutTimerOptions.circularReveal(binding.buttonTimer)

    fun closeTimerAndSelect(timer: CameraTimer) =
        binding.layoutTimerOptions.circularClose(binding.buttonTimer) {
            binding.buttonTimer.setImageResource(
                when (timer) {
                    CameraTimer.S3 -> {
                        selectedTimer = CameraTimer.S3
                        R.drawable.ic_timer_3
                    }
                    CameraTimer.S10 -> {
                        selectedTimer = CameraTimer.S10
                        R.drawable.ic_timer_10
                    }
                    else -> {
                        selectedTimer = CameraTimer.OFF
                        R.drawable.ic_timer_off
                    }
                }
            )
        }

    fun selectFlash() = binding.layoutFlashOptions.circularReveal(binding.buttonFlash)

    fun closeFlashAndSelect(flash: FlashMode) =
        binding.layoutFlashOptions.circularClose(binding.buttonFlash) {
            flashMode = when (flash) {
                FlashMode.ON -> FlashMode.ON.ordinal
                FlashMode.AUTO -> FlashMode.AUTO.ordinal
                else -> FlashMode.OFF.ordinal
            }
            prefs.putInt(KEY_FLASH, flashMode)
            imageCapture.flashMode = getFlashMode()
        }

    fun toggleGrid() {
        binding.buttonGrid.toggleButton(hasGrid, 180f, R.drawable.ic_grid_off, R.drawable.ic_grid_on) { flag ->
            hasGrid = flag
            prefs.putBoolean(KEY_GRID, flag)
            binding.groupGridLines.visibility = if (flag) View.VISIBLE else View.GONE
        }
    }

    override fun onPermissionGranted() {
        binding.viewFinder.let { vf ->
            vf.post {
                displayId = vf.display.displayId
                recreateCamera()
                lifecycleScope.launch(Dispatchers.IO) {
                    outputDirectory.listFiles()?.firstOrNull()?.let {
                        setGalleryThumbnail(it)
                    } ?: binding.buttonGallery.setImageResource(R.drawable.ic_no_picture)
                }
            }
        }
    }

    private fun startCamera() {
        val viewFinder = binding.viewFinder

        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val ratio = Rational(metrics.widthPixels, metrics.heightPixels)

        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(ratio)
            setLensFacing(lensFacing)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        preview = AutoFitPreviewBuilder.build(previewConfig, viewFinder)

        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetAspectRatio(ratio)
            setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            setTargetRotation(viewFinder.display.rotation)
            setFlashMode(getFlashMode())
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        binding.fabTakePicture.setOnClickListener { takePicture(imageCapture) }

        CameraX.bindToLifecycle(viewLifecycleOwner, preview, imageCapture)
    }

    private fun getFlashMode() = when (flashMode) {
        FlashMode.ON.ordinal -> FlashMode.ON
        FlashMode.AUTO.ordinal -> FlashMode.AUTO
        else -> FlashMode.OFF
    }

    @Suppress("NON_EXHAUSTIVE_WHEN")
    private fun takePicture(imageCapture: ImageCapture) = lifecycleScope.launch(Dispatchers.Main) {
        when (selectedTimer) {
            CameraTimer.S3 -> for (i in 3 downTo 1) {
                binding.textCountDown.text = i.toString()
                delay(1000)
            }
            CameraTimer.S10 -> for (i in 10 downTo 1) {
                binding.textCountDown.text = i.toString()
                delay(1000)
            }
        }
        binding.textCountDown.text = ""
        captureImage(imageCapture)
    }

    private fun captureImage(imageCapture: ImageCapture) {
        val imageFile = File(outputDirectory, "${System.currentTimeMillis()}.jpg")
        imageCapture.takePicture(imageFile, object : ImageCapture.OnImageSavedListener {
            override fun onImageSaved(file: File) {
                setGalleryThumbnail(file)
                val msg = "Photo saved in ${file.absolutePath}"
                Log.d("CameraXDemo", msg)
            }

            override fun onError(useCaseError: ImageCapture.UseCaseError, message: String, cause: Throwable?) {
                val msg = "Photo capture failed: $message"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                Log.e("CameraXApp", msg)
                cause?.printStackTrace()
            }
        })
    }

    private fun setGalleryThumbnail(file: File) = binding.buttonGallery.let {
        it.post {
            Glide.with(requireContext())
                .load(file)
                .apply(RequestOptions.circleCropTransform())
                .into(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onBackPressed() {
        when {
            binding.layoutTimerOptions.visibility == View.VISIBLE -> binding.layoutTimerOptions.circularClose(binding.buttonTimer)
            binding.layoutFlashOptions.visibility == View.VISIBLE -> binding.layoutFlashOptions.circularClose(binding.buttonFlash)
            else -> requireActivity().finish()
        }
    }
}