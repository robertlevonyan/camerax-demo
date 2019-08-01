package com.robertlevonyan.demo.camerax.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.robertlevonyan.demo.camerax.BaseFragment
import com.robertlevonyan.demo.camerax.R
import com.robertlevonyan.demo.camerax.utils.AutoFitPreviewBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class CameraFragment : BaseFragment() {

    companion object {
        private const val TAG = "CameraFragment"

        fun newInstance() = CameraFragment()
    }

    private lateinit var viewModel: CameraViewModel
    private lateinit var displayManager: DisplayManager
    private lateinit var fabTakePicture: FloatingActionButton
    private lateinit var viewFinder: TextureView
    private lateinit var buttonSwitchCamera: ImageButton

    private var displayId = -1
    private var lensFacing = CameraX.LensFacing.BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                preview?.setTargetRotation(view.display.rotation)
                imageCapture?.setTargetRotation(view.display.rotation)
                imageAnalyzer?.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CameraViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_camera, container, false) ?: null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)

        displayManager = viewFinder.context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        viewFinder.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(p0: View?) =
                displayManager.registerDisplayListener(displayListener, null)

            override fun onViewAttachedToWindow(p0: View?) =
                displayManager.unregisterDisplayListener(displayListener)

        })
    }

    private fun initViews(view: View) {
        fabTakePicture = view.findViewById(R.id.fabTakePicture)
        viewFinder = view.findViewById(R.id.viewFinder)
        buttonSwitchCamera = view.findViewById(R.id.buttonSwitchCamera)

        buttonSwitchCamera.setOnClickListener { toggleCamera() }

        ViewCompat.setOnApplyWindowInsetsListener(fabTakePicture) { v, insets ->
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = insets.systemWindowInsetBottom * 2
            insets.consumeSystemWindowInsets()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun toggleCamera() {
        lensFacing = if (lensFacing == CameraX.LensFacing.FRONT) {
            buttonSwitchCamera.animate().rotationY(0f).duration = 200
            lifecycleScope.launch(Dispatchers.Main) {
                delay(100)
                buttonSwitchCamera.setImageResource(R.drawable.ic_outline_camera_front)
            }
            CameraX.LensFacing.BACK
        } else {
            buttonSwitchCamera.animate().rotationY(180f).duration = 200
            lifecycleScope.launch(Dispatchers.Main) {
                delay(100)
                buttonSwitchCamera.setImageResource(R.drawable.ic_outline_camera_rear)
            }
            CameraX.LensFacing.FRONT
        }
        CameraX.getCameraWithLensFacing(lensFacing)
        CameraX.unbindAll()
        startCamera()
    }

    override fun onPermissionGranted() {
        viewFinder.post {
            displayId = viewFinder.display.displayId
            startCamera()
        }
    }

    private fun startCamera() {
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val realAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels) // todo add also 4:3 and 1:1

        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(realAspectRatio)
            setLensFacing(lensFacing)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        preview = AutoFitPreviewBuilder.build(previewConfig, viewFinder)

        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetAspectRatio(realAspectRatio)
            setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        fabTakePicture.setOnClickListener {
            captureImage(imageCapture)
        }

        CameraX.bindToLifecycle(viewLifecycleOwner, preview/*, imageCapture*/)
    }

    private fun captureImage(imageCapture: ImageCapture?) {
        val file = File(requireContext().externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
        imageCapture?.takePicture(file, object : ImageCapture.OnImageSavedListener {
            override fun onImageSaved(file: File) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        displayManager.unregisterDisplayListener(displayListener)
    }
}