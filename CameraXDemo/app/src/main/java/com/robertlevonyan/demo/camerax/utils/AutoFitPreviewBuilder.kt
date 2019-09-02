package com.robertlevonyan.demo.camerax.utils

import android.graphics.Matrix
import android.util.Size
import android.view.Display
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.roundToInt

/**
 * Builder for [Preview] that takes in a [WeakReference] of the view finder and [PreviewConfig],
 * then instantiates a [Preview] which automatically resizes and rotates reacting to config changes.
 */
class AutoFitPreviewBuilder private constructor(config: PreviewConfig, viewFinderRef: WeakReference<TextureView>) {
    val previewUseCase: Preview
    private var bufferRotation = 0
    private var viewFinderRotation = 0
    private var bufferDimens = Size(0, 0)
    private var viewFinderDimens = Size(0, 0)
    private var viewFinderDisplay = -1

    init {
        // Make sure that the view finder reference is valid
        val viewFinder = viewFinderRef.get()
                ?: throw IllegalArgumentException("Invalid reference to view finder used")

        // Initialize the display and rotation from texture view information
        viewFinderDisplay = viewFinder.display.displayId
        viewFinderRotation = getDisplaySurfaceRotation(viewFinder.display)

        // Initialize public use-case with the given config
        previewUseCase = Preview(config)

        // Every time the view finder is updated, recompute layout
        previewUseCase.setOnPreviewOutputUpdateListener { output ->
            val vFinder = viewFinderRef.get() ?: return@setOnPreviewOutputUpdateListener

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            // Update internal texture
            viewFinder.surfaceTexture = output.surfaceTexture
            bufferRotation = output.rotationDegrees
            val rotation = getDisplaySurfaceRotation(viewFinder.display)
            updateTransform(vFinder, rotation, output.textureSize, viewFinderDimens)
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
            val vFinder = view as TextureView
            val newViewFinderDimens = Size(right - left, bottom - top)
            val rotation = getDisplaySurfaceRotation(viewFinder.display)
            updateTransform(vFinder, rotation, bufferDimens, newViewFinderDimens)
        }
    }

    /** Helper function that fits a camera preview into the given [TextureView] */
    private fun updateTransform(vFinder: TextureView, rotation: Int, newBufferDimens: Size, newViewFinderDimens: Size) {
        if (rotation == viewFinderRotation &&
                Objects.equals(newBufferDimens, bufferDimens) &&
                Objects.equals(newViewFinderDimens, viewFinderDimens)
        ) return

        viewFinderRotation = rotation

        if (newBufferDimens.width == 0 || newBufferDimens.height == 0) return
        bufferDimens = newBufferDimens

        if (newViewFinderDimens.width == 0 || newViewFinderDimens.height == 0) return
        viewFinderDimens = newViewFinderDimens

        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinderDimens.width / 2f
        val centerY = viewFinderDimens.height / 2f

        matrix.postRotate(-viewFinderRotation.toFloat(), centerX, centerY)

        val bufferRatio = bufferDimens.height / bufferDimens.width.toFloat()

        val scaledWidth: Int
        val scaledHeight: Int
        if (viewFinderDimens.width > viewFinderDimens.height) {
            scaledHeight = viewFinderDimens.width
            scaledWidth = (viewFinderDimens.width * bufferRatio).roundToInt()
        } else {
            scaledHeight = viewFinderDimens.height
            scaledWidth = (viewFinderDimens.height * bufferRatio).roundToInt()
        }

        val xScale = scaledWidth / viewFinderDimens.width.toFloat()
        val yScale = scaledHeight / viewFinderDimens.height.toFloat()

        matrix.preScale(xScale, yScale, centerX, centerY)

        vFinder.setTransform(matrix)
    }

    companion object {
        /** Helper function that gets the rotation of a [Display] in degrees */
        fun getDisplaySurfaceRotation(display: Display?) = when (display?.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        /**
         * Returns an instance of [Preview] which automatically adjusts in size and rotation to
         * compensate for config changes.
         */
        fun build(config: PreviewConfig, viewFinder: TextureView) =
                AutoFitPreviewBuilder(config, WeakReference(viewFinder)).previewUseCase
    }

}