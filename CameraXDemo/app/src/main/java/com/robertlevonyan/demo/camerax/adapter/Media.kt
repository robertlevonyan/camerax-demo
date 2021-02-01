package com.robertlevonyan.demo.camerax.adapter

import android.net.Uri
import java.util.*

data class Media(
    val uri: Uri,
    val isVideo: Boolean,
    val date: Long,
)
