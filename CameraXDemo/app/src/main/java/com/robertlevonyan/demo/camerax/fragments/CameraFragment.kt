package com.robertlevonyan.demo.camerax.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.robertlevonyan.demo.camerax.R
import com.robertlevonyan.demo.camerax.databinding.FragmentCameraBinding
import com.robertlevonyan.demo.camerax.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class CameraFragment : BaseFragment<FragmentCameraBinding>(R.layout.fragment_camera) {
    companion object {
        private const val TAG = "CameraFragment"
        private const val KEY_FLASH = "sPrefFlash"
        private const val KEY_HDR = "sPrefHdr"
        private const val KEY_GRID = "sPrefGrid"
    }

    private lateinit var displayManager: DisplayManager
    private lateinit var prefs: SharedPrefsManager
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalyzer: ImageAnalysis
    private lateinit var outputDirectory: File

    private var displayId = -1
    private var lensFacing = CameraX.LensFacing.BACK
    private var flashMode = FlashMode.OFF.ordinal
    private var hasHDR = false
    private var hasGrid = false
    private var selectedTimer = CameraTimer.OFF

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                preview.setTargetRotation(view.display.rotation)
                imageCapture.setTargetRotation(view.display.rotation)
                imageAnalyzer.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = SharedPrefsManager.newInstance(requireContext())
        flashMode = prefs.getInt(KEY_FLASH, FlashMode.OFF.ordinal)
        hasHDR = prefs.getBoolean(KEY_HDR, false)
        hasGrid = prefs.getBoolean(KEY_GRID, false)
        outputDirectory = File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath
                ?: requireContext().externalMediaDirs.first().absolutePath
        )
        initViews()

        displayManager = requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        binding.viewFinder.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View) =
                displayManager.registerDisplayListener(displayListener, null)

            override fun onViewAttachedToWindow(v: View) = displayManager.unregisterDisplayListener(displayListener)
        })
    }

    private fun initViews() {
        binding.buttonHDR.setImageResource(if (hasHDR) R.drawable.ic_hdr_on else R.drawable.ic_hdr_off)
        binding.buttonGrid.setImageResource(if (hasGrid) R.drawable.ic_grid_on else R.drawable.ic_grid_off)
        binding.groupGridLines.visibility = if (hasGrid) View.VISIBLE else View.GONE

        binding.buttonSwitchCamera.setOnClickListener { toggleCamera() }
        binding.buttonGallery.setOnClickListener { openPreview(it) }
        binding.buttonTimer.setOnClickListener { selectTimer() }
        binding.buttonTimerOff.setOnClickListener { closeTimerAndSelect(CameraTimer.OFF) }
        binding.buttonTimer3.setOnClickListener { closeTimerAndSelect(CameraTimer.S3) }
        binding.buttonTimer10.setOnClickListener { closeTimerAndSelect(CameraTimer.S10) }
        binding.buttonHDR.setOnClickListener { toggleHDR() }
        binding.buttonFlash.setOnClickListener { selectFlash() }
        binding.buttonFlashOff.setOnClickListener { closeFlashAndSelect(FlashMode.OFF) }
        binding.buttonFlashOn.setOnClickListener { closeFlashAndSelect(FlashMode.ON) }
        binding.buttonFlashAuto.setOnClickListener { closeFlashAndSelect(FlashMode.AUTO) }
        binding.buttonGrid.setOnClickListener { toggleGrid() }

        adjustInsets()
    }

    @SuppressLint("RestrictedApi")
    private fun toggleCamera() {
        lensFacing = if (lensFacing == CameraX.LensFacing.FRONT) {
            binding.buttonSwitchCamera.animate().rotationY(0f).duration = 200
            lifecycleScope.launch(Dispatchers.Main) {
                delay(100)
                binding.buttonSwitchCamera.setImageResource(R.drawable.ic_outline_camera_front)
            }
            CameraX.LensFacing.BACK
        } else {
            binding.buttonSwitchCamera.animate().rotationY(180f).duration = 200
            lifecycleScope.launch(Dispatchers.Main) {
                delay(100)
                binding.buttonSwitchCamera.setImageResource(R.drawable.ic_outline_camera_rear)
            }
            CameraX.LensFacing.FRONT
        }
        CameraX.getCameraWithLensFacing(lensFacing)
        recreateCamera()
    }

    private fun recreateCamera() {
        CameraX.unbindAll()
        startCamera()
    }

    private fun openPreview(view: View) {
        Navigation.findNavController(view).navigate(R.id.action_camera_to_preview)
    }

    private fun selectTimer() = binding.layoutTimerOptions.circularReveal(binding.buttonTimer)

    private fun closeTimerAndSelect(timer: CameraTimer) =
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

    private fun toggleHDR() {
        binding.buttonHDR.toggleButton(hasHDR, 360f, R.drawable.ic_hdr_off, R.drawable.ic_hdr_on) { flag ->
            hasHDR = flag
            prefs.putBoolean(KEY_HDR, flag)
        }
    }

    private fun selectFlash() = binding.layoutFlashOptions.circularReveal(binding.buttonFlash)

    private fun closeFlashAndSelect(flash: FlashMode) =
        binding.layoutFlashOptions.circularClose(binding.buttonFlash) {
            binding.buttonFlash.setImageResource(
                when (flash) {
                    FlashMode.ON -> {
                        flashMode = FlashMode.ON.ordinal
                        R.drawable.ic_flash_on
                    }
                    FlashMode.AUTO -> {
                        flashMode = FlashMode.AUTO.ordinal
                        R.drawable.ic_flash_auto
                    }
                    else -> {
                        flashMode = FlashMode.OFF.ordinal
                        R.drawable.ic_flash_off
                    }
                }
            )
            prefs.putInt(KEY_FLASH, flashMode)
            recreateCamera()
        }

    private fun toggleGrid() {
        binding.buttonGrid.toggleButton(hasGrid, 180f, R.drawable.ic_grid_off, R.drawable.ic_grid_on) { flag ->
            hasGrid = flag
            prefs.putBoolean(KEY_GRID, flag)
            binding.groupGridLines.visibility = if (flag) View.VISIBLE else View.GONE
        }
    }

    private fun adjustInsets() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            ViewCompat.requestApplyInsets(binding.fabTakePicture)
            ViewCompat.requestApplyInsets(binding.buttonHDR)
            ViewCompat.requestApplyInsets(binding.layoutTimerOptions)
            ViewCompat.setOnApplyWindowInsetsListener(binding.fabTakePicture) { v, insets ->
                val params = v.layoutParams as ViewGroup.MarginLayoutParams
                params.bottomMargin = insets.systemWindowInsetBottom * 2
                insets.consumeSystemWindowInsets()
            }
            ViewCompat.setOnApplyWindowInsetsListener(binding.buttonHDR) { v, insets ->
                val params = v.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = insets.systemWindowInsetTop * 2
                insets.consumeSystemWindowInsets()
            }
            ViewCompat.setOnApplyWindowInsetsListener(binding.layoutTimerOptions) { v, insets ->
                val params = v.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = insets.systemWindowInsetTop * 2
                insets.consumeSystemWindowInsets()
            }
        }
    }

    override fun onPermissionGranted() {
        val viewFinder = binding.viewFinder

        viewFinder.post {
            displayId = binding.viewFinder.display.displayId
            recreateCamera()
        }
    }

    private fun startCamera() {
        val viewFinder = binding.viewFinder

        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val ratio = Rational(metrics.heightPixels, metrics.widthPixels)

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

        lifecycleScope.launch(Dispatchers.IO) {
            outputDirectory.listFiles()?.firstOrNull()?.let {
                setGalleryThumbnail(it)
            } ?: binding.buttonGallery.setImageResource(R.drawable.ic_no_picture)
        }
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
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
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

    private fun setGalleryThumbnail(file: File) {
        val buttonGallery = binding.buttonGallery
        buttonGallery.post {
            Glide.with(requireContext())
                .load(file)
                .apply(RequestOptions.circleCropTransform())
                .into(buttonGallery)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        displayManager.unregisterDisplayListener(displayListener)
    }
}