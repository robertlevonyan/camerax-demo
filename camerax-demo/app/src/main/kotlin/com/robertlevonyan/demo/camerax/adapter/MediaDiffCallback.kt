package com.robertlevonyan.demo.camerax.adapter

import androidx.recyclerview.widget.DiffUtil

class MediaDiffCallback : DiffUtil.ItemCallback<Media>() {
    override fun areItemsTheSame(oldItem: Media, newItem: Media): Boolean = oldItem.uri == newItem.uri

    override fun areContentsTheSame(oldItem: Media, newItem: Media): Boolean = oldItem == newItem
}
