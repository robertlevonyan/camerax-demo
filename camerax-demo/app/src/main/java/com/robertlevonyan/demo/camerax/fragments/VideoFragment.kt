package com.robertlevonyan.demo.camerax.fragments

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.animation.doOnCancel
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import coil.load
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.robertlevonyan.demo.camerax.R
import com.robertlevonyan.demo.camerax.databinding.FragmentVideoBinding
import com.robertlevonyan.demo.camerax.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates


@ExperimentalCamera2Interop
@SuppressLint("RestrictedApi")
class VideoFragment : BaseFragment<FragmentVideoBinding>(R.layout.fragment_video) {
    // An instance for display manager to get display change callbacks
    private val displayManager by lazy { requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }

    // An instance of a helper function to work with Shared Preferences
    private val prefs by lazy { SharedPrefsManager.newInstance(requireContext()) }

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null

    private var displayId = -1

    // Selector showing which camera is selected (front or back)
    private var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA

    // Selector showing which flash mode is selected (on, off or auto)
    private var flashMode by Delegates.observable(ImageCapture.FLASH_MODE_OFF) { _, _, new ->
        binding.btnFlash.setImageResource(
            when (new) {
                ImageCapture.FLASH_MODE_ON -> R.drawable.ic_flash_on
                ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_flash_auto
                else -> R.drawable.ic_flash_off
            }
        )
    }

    // Selector showing is grid enabled or not
    private var hasGrid = false

    // Selector showing is flash enabled or not
    private var isTorchOn = false

    // Selector showing is recording currently active
    private var isRecording = false
    private val animateRecord by lazy {
        ObjectAnimator.ofFloat(binding.btnRecordVideo, View.ALPHA, 1f, 0.5f).apply {
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            doOnCancel { binding.btnRecordVideo.alpha = 1f }
        }
    }

    // A lazy instance of the current fragment's view binding
    override val binding: FragmentVideoBinding by lazy { FragmentVideoBinding.inflate(layoutInflater) }

    /**
     * A display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit

        @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@VideoFragment.displayId) {
                preview?.targetRotation = view.display.rotation
                videoCapture?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hasGrid = prefs.getBoolean(KEY_GRID, false)
        initViews()

        displayManager.registerDisplayListener(displayListener, null)

        binding.run {
            viewFinder.addOnAttachStateChangeListener(object :
                View.OnAttachStateChangeListener {
                override fun onViewDetachedFromWindow(v: View) =
                    displayManager.registerDisplayListener(displayListener, null)

                override fun onViewAttachedToWindow(v: View) =
                    displayManager.unregisterDisplayListener(displayListener)
            })
            binding.btnRecordVideo.setOnClickListener { recordVideo() }
            btnGallery.setOnClickListener { openPreview() }
            btnSwitchCamera.setOnClickListener { toggleCamera() }
            btnGrid.setOnClickListener { toggleGrid() }
            btnFlash.setOnClickListener { toggleFlash() }

            // This swipe gesture adds a fun gesture to switch between video and photo
            val swipeGestures = SwipeGestureDetector().apply {
                setSwipeCallback(left = {
                    Navigation.findNavController(view).navigate(R.id.action_video_to_camera)
                })
            }

            val gestureDetectorCompat = GestureDetector(requireContext(), swipeGestures)
            viewFinder.setOnTouchListener { _, motionEvent ->
                if (gestureDetectorCompat.onTouchEvent(motionEvent)) return@setOnTouchListener false
                return@setOnTouchListener true
            }
        }
    }

    /**
     * Create some initial states
     * */
    private fun initViews() {
        binding.btnGrid.setImageResource(if (hasGrid) R.drawable.ic_grid_on else R.drawable.ic_grid_off)
        binding.groupGridLines.visibility = if (hasGrid) View.VISIBLE else View.GONE

        adjustInsets()
    }

    /**
     * This methods adds all necessary margins to some views based on window insets and screen orientation
     * */
    private fun adjustInsets() {
        activity?.window?.fitSystemWindows()
        binding.btnRecordVideo.onWindowInsets { view, windowInsets ->
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                view.bottomMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            } else {
                view.endMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).right
            }
        }
        binding.btnFlash.onWindowInsets { view, windowInsets ->
            view.topMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top
        }
    }

    /**
     * Change the facing of camera
     *  toggleButton() function is an Extension function made to animate button rotation
     * */
    private fun toggleCamera() = binding.btnSwitchCamera.toggleButton(
        flag = lensFacing == CameraSelector.DEFAULT_BACK_CAMERA,
        rotationAngle = 180f,
        firstIcon = R.drawable.ic_outline_camera_rear,
        secondIcon = R.drawable.ic_outline_camera_front,
    ) {
        lensFacing = if (it) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        startCamera()
    }

    /**
     * Unbinds all the lifecycles from CameraX, then creates new with new parameters
     * */
    private fun startCamera() {
        // This is the Texture View where the camera will be rendered
        val viewFinder = binding.viewFinder

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // The display information
            val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
            // The ratio for the output image and preview
            val aspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
            // The display rotation
            val rotation = viewFinder.display.rotation

            val localCameraProvider = cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

            // The Configuration of camera preview
            preview = Preview.Builder()
                .setTargetAspectRatio(aspectRatio) // set the camera aspect ratio
                .setTargetRotation(rotation) // set the camera rotation
                .build()

            val cameraInfo = localCameraProvider.availableCameraInfos.filter {
                Camera2CameraInfo
                    .from(it)
                    .getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK
            }

            val supportedQualities = QualitySelector.getSupportedQualities(cameraInfo[0])
            val qualitySelector = QualitySelector.fromOrderedList(
                listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )
            val recorder = Recorder.Builder()
                .setExecutor(ContextCompat.getMainExecutor(requireContext())).setQualitySelector(qualitySelector)
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            localCameraProvider.unbindAll() // unbind the use-cases before rebinding them

            try {
                // Bind all use cases to the camera with lifecycle
                camera = localCameraProvider.bindToLifecycle(
                    viewLifecycleOwner, // current lifecycle owner
                    lensFacing, // either front or back facing
                    preview, // camera preview use case
                    videoCapture, // video capture use case
                )

                // Attach the viewfinder's surface provider to preview use case
                preview?.setSurfaceProvider(viewFinder.surfaceProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind use cases", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     *  Detecting the most suitable aspect ratio for current dimensions
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /**
     * Navigate to PreviewFragment
     * */
    private fun openPreview() {
        view?.let { Navigation.findNavController(it).navigate(R.id.action_video_to_preview) }
    }

    var recording: Recording? = null

    @SuppressLint("MissingPermission")
    private fun recordVideo() {
        if (recording != null) {
            animateRecord.cancel()
            recording?.stop()
        }
        val name = "CameraX-recording-${System.currentTimeMillis()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()
        recording = videoCapture?.output
            ?.prepareRecording(requireContext(), mediaStoreOutput)
            ?.withAudioEnabled()
            ?.start(ContextCompat.getMainExecutor(requireContext())) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        animateRecord.start()
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${event.outputResults.outputUri}"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${event.error}")
                        }
                    }
                }
            }
        isRecording = !isRecording
    }

    /**
     * Turns on or off the grid on the screen
     * */
    private fun toggleGrid() = binding.btnGrid.toggleButton(
        flag = hasGrid,
        rotationAngle = 180f,
        firstIcon = R.drawable.ic_grid_off,
        secondIcon = R.drawable.ic_grid_on
    ) { flag ->
        hasGrid = flag
        prefs.putBoolean(KEY_GRID, flag)
        binding.groupGridLines.visibility = if (flag) View.VISIBLE else View.GONE
    }

    /**
     * Turns on or off the flashlight
     * */
    private fun toggleFlash() = binding.btnFlash.toggleButton(
        flag = flashMode == ImageCapture.FLASH_MODE_ON,
        rotationAngle = 360f,
        firstIcon = R.drawable.ic_flash_off,
        secondIcon = R.drawable.ic_flash_on
    ) { flag ->
        isTorchOn = flag
        flashMode = if (flag) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        camera?.cameraControl?.enableTorch(flag)
    }

    override fun onPermissionGranted() {
        // Each time apps is coming to foreground the need permission check is being processed
        binding.viewFinder.let { vf ->
            vf.post {
                // Setting current display ID
                displayId = vf.display.displayId
                startCamera()
                lifecycleScope.launch(Dispatchers.IO) {
                    // Do on IO Dispatcher
                    setLastPictureThumbnail()
                }
                camera?.cameraControl?.enableTorch(isTorchOn)
            }
        }
    }

    private fun setLastPictureThumbnail() = binding.btnGallery.post {
        getMedia().firstOrNull() // check if there are any photos or videos in the app directory
            ?.let { setGalleryThumbnail(it.uri) } // preview the last one
            ?: binding.btnGallery.setImageResource(R.drawable.ic_no_picture) // or the default placeholder
    }

    private fun setGalleryThumbnail(savedUri: Uri?) = binding.btnGallery.let { btnGallery ->
        // Do the work on view's thread, this is needed, because the function is called in a Coroutine Scope's IO Dispatcher
        btnGallery.post {
            btnGallery.load(savedUri) {
                placeholder(R.drawable.ic_no_picture)
                transformations(CircleCropTransformation())
                listener(object : ImageRequest.Listener {
                    override fun onError(request: ImageRequest, result: ErrorResult) {
                        super.onError(request, result)
                        binding.btnGallery.load(savedUri) {
                            placeholder(R.drawable.ic_no_picture)
                            transformations(CircleCropTransformation())
//                            fetcher(VideoFrameUriFetcher(requireContext()))
                        }
                    }
                })
            }
        }
    }

    override fun onBackPressed() = requireActivity().finish()

    override fun onStop() {
        super.onStop()
        camera?.cameraControl?.enableTorch(false)
    }

    companion object {
        private const val TAG = "CameraXDemo"

        const val KEY_GRID = "sPrefGridVideo"

        private const val RATIO_4_3_VALUE = 4.0 / 3.0 // aspect ratio 4x3
        private const val RATIO_16_9_VALUE = 16.0 / 9.0 // aspect ratio 16x9
    }
}
