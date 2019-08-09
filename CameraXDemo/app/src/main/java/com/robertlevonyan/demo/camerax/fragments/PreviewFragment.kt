package com.robertlevonyan.demo.camerax.fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import com.robertlevonyan.demo.camerax.R
import com.robertlevonyan.demo.camerax.adapter.PicturesAdapter
import com.robertlevonyan.demo.camerax.databinding.FragmentPreviewBinding
import com.robertlevonyan.demo.camerax.utils.*

class PreviewFragment : BaseFragment<FragmentPreviewBinding>(R.layout.fragment_preview) {
    private lateinit var picturesAdapter: PicturesAdapter
    private var currentPage = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fragment = this
        adjustInsets()

        if (allPermissionsGranted()) {
            outputDirectory.listFiles()?.let {
                picturesAdapter = PicturesAdapter(it.toMutableList()) {
                    binding.groupPreviewActions.visibility =
                        if (binding.groupPreviewActions.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
                binding.pagerPhotos.apply {
                    adapter = picturesAdapter
                    onPageSelected { page -> currentPage = page }
                }
            }
        }
    }

    private fun adjustInsets() {
        binding.layoutRoot.fitSystemWindows()
        binding.imageBack.onWindowInsets { view, windowInsets ->
            view.topMargin = windowInsets.systemWindowInsetTop
        }
        binding.imageShare.onWindowInsets { view, windowInsets ->
            view.bottomMargin = windowInsets.systemWindowInsetBottom
        }
    }

    override fun onPermissionGranted() = Unit

    fun shareImage() {
        if (!::picturesAdapter.isInitialized) return

        picturesAdapter.shareImage(currentPage) { share(it) }
    }

    fun deleteImage() {
        if (!::picturesAdapter.isInitialized) return

        picturesAdapter.deleteImage(currentPage) {
            if (outputDirectory.listFiles().isNullOrEmpty()) onBackPressed()
        }
    }

    override fun onBackPressed() {
        view?.let { Navigation.findNavController(it).popBackStack() }
    }

}