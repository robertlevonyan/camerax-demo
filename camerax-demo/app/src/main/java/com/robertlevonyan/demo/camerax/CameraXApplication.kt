package com.robertlevonyan.demo.camerax

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

class CameraXApplication: Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            add(VideoFrameDecoder.Factory())
        }
        .build()
}