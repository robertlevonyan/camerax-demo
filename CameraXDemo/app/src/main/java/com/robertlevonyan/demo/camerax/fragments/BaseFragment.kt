package com.robertlevonyan.demo.camerax.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.robertlevonyan.demo.camerax.R
import java.io.File
import kotlin.properties.Delegates

/**Parent class of all the fragments in this project*/
abstract class BaseFragment<B : ViewDataBinding>(private val fragmentLayout: Int) : Fragment() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    protected lateinit var binding: B // Generic ViewBinding of the subclasses
    protected lateinit var outputDirectory: File // The Folder where all the files will be stored

    // The permissions we need for the app to work properly
    private val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If the fragment is retained, the lifecycle does not get restarted it on config change
        retainInstance = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Adding an option to handle the back press in fragment
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        })

        // Init the output folder
        outputDirectory = File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath
                        ?: requireContext().externalMediaDirs.first().absolutePath
        )

        // Create a binding instance
        binding = DataBindingUtil.inflate(inflater, fragmentLayout, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Each time the app comes to foreground we will check for the permissions
        if (allPermissionsGranted()) {
            onPermissionGranted()
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(), permissions,
                    REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                onPermissionGranted()
            } else {
                view?.let { v ->
                    Snackbar.make(v, R.string.message_no_permissions, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.label_ok) { ActivityCompat.finishAffinity(requireActivity()) }
                            .show()
                }
            }
        }
    }

    // Check for the permissions
    protected fun allPermissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    open fun onPermissionGranted() = Unit // a function which will be called after the permission check

    abstract fun onBackPressed() // an abstract function which will be called on the Back button press

}