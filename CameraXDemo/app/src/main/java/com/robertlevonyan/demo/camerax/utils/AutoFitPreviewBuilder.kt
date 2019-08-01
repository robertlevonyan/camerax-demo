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

class AutoFitPreviewBuilder private constructor(config: PreviewConfig, viewFinderRef: WeakReference<TextureView>) {
    val previewUseCase: Preview
    private var bufferRotation = 0
    private var viewFinderRotation = 0
    private var bufferDimens = Size(0, 0)
    private var viewFinderDimens = Size(0, 0)
    private var viewFinderDisplay = -1

    init {
        val viewFinder = viewFinderRef.get() ?: throw IllegalArgumentException("Invalid reference to view finder used")

        viewFinderDisplay = viewFinder.display.displayId
        viewFinderRotation = getDisplaySurfaceRotation(viewFinder.display)

        previewUseCase = Preview(config)

        previewUseCase.setOnPreviewOutputUpdateListener { output ->
            val vFinder = viewFinderRef.get() ?: return@setOnPreviewOutputUpdateListener

            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = output.surfaceTexture
            bufferRotation = output.rotationDegrees
            val rotation = getDisplaySurfaceRotation(viewFinder.display)
            updateTransform(vFinder, rotation, output.textureSize, viewFinderDimens)
        }

        viewFinder.addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
            val vFinder = view as TextureView
            val newViewFinderDimens = Size(right - left, bottom - top)
            val rotation = getDisplaySurfaceRotation(viewFinder.display)
            updateTransform(vFinder, rotation, bufferDimens, newViewFinderDimens)
        }
    }

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
        fun getDisplaySurfaceRotation(display: Display?) = when (display?.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        fun build(config: PreviewConfig, viewFinder: TextureView) =
            AutoFitPreviewBuilder(config, WeakReference(viewFinder)).previewUseCase
    }

}