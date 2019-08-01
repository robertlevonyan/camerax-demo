package com.robertlevonyan.demo.camerax

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

abstract class BaseFragment : Fragment() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    private val PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            onPermissionGranted()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), PERMISSIONS, REQUEST_CODE_PERMISSIONS)
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

    private fun allPermissionsGranted() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    abstract fun onPermissionGranted()
}