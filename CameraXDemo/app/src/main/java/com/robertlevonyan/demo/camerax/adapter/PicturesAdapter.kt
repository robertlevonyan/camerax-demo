package com.robertlevonyan.demo.camerax.adapter

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.robertlevonyan.demo.camerax.R
import com.robertlevonyan.demo.camerax.utils.layoutInflater
import java.io.File

/**
 * This is and adapter to preview taken photos or videos
 * */
class PicturesAdapter(private val files: MutableList<File>, private val click: (Boolean, Uri) -> Unit) :
    RecyclerView.Adapter<PicturesAdapter.PicturesViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PicturesViewHolder(parent.context.layoutInflater.inflate(R.layout.item_picture, parent, false))

    override fun getItemCount() = files.size

    override fun onBindViewHolder(holder: PicturesViewHolder, position: Int) {
        val file = files[position]
        val isVideo = file.extension == "mp4"
        holder.imagePlay.visibility = if (isVideo) View.VISIBLE else View.GONE
        holder.imagePreview.let {
            Glide.with(it)
                .load(file)
                .into(it)
            it.setOnClickListener { click(isVideo, Uri.parse(file.absolutePath)) }
        }
    }

    fun shareImage(currentPage: Int, action: (File) -> Unit) {
        if (currentPage < files.size) {
            action(files[currentPage])
        }
    }

    fun deleteImage(currentPage: Int, action: () -> Unit) {
        if (currentPage < files.size) {
            val picture = files[currentPage]
            if (picture.exists() && picture.delete()) {
                files.removeAt(currentPage)
                notifyItemRemoved(currentPage)
                action()
            }
        }
    }

    class PicturesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagePreview: ImageView = itemView.findViewById(R.id.imagePreview)
        val imagePlay: ImageView = itemView.findViewById(R.id.imagePlay)
    }
}