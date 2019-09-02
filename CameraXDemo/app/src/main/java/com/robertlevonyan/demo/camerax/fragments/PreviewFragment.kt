package com.robertlevonyan.demo.camerax.fragments

import android.content.Intent
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
        binding.fragment = this // setting the variable for XML
        adjustInsets()

        // Check for the permissions and show files
        if (allPermissionsGranted()) {
            outputDirectory.listFiles()?.let {
                picturesAdapter = PicturesAdapter(it.toMutableList()) { isVideo, uri ->
                    if (!isVideo) {
                        binding.groupPreviewActions.visibility =
                            if (binding.groupPreviewActions.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    } else {
                        val play = Intent(Intent.ACTION_VIEW, uri).apply { setDataAndType(uri, "video/mp4") }
                        startActivity(play)
                    }
                }
                binding.pagerPhotos.apply {
                    adapter = picturesAdapter
                    onPageSelected { page -> currentPage = page }
                }
            }
        }
    }

    /**
     * This methods adds all necessary margins to some views based on window insets and screen orientation
     * */
    private fun adjustInsets() {
        binding.layoutRoot.fitSystemWindows()
        binding.imageBack.onWindowInsets { view, windowInsets ->
            view.topMargin = windowInsets.systemWindowInsetTop
        }
        binding.imageShare.onWindowInsets { view, windowInsets ->
            view.bottomMargin = windowInsets.systemWindowInsetBottom
        }
    }

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