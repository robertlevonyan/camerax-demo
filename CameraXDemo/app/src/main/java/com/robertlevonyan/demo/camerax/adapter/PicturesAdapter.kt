package com.robertlevonyan.demo.camerax.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.robertlevonyan.demo.camerax.R
import com.robertlevonyan.demo.camerax.utils.layoutInflater
import java.io.File

class PicturesAdapter(private val pictures: MutableList<File>, private val click: () -> Unit) :
    RecyclerView.Adapter<PicturesAdapter.PicturesViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PicturesViewHolder(parent.context.layoutInflater.inflate(R.layout.item_picture, parent, false))

    override fun getItemCount() = pictures.size

    override fun onBindViewHolder(holder: PicturesViewHolder, position: Int) {
        val picture = pictures[position]
        holder.imagePreview.let {
            Glide.with(it)
                .load(picture)
                .into(it)
            it.setOnClickListener { click() }
        }
    }

    fun shareImage(currentPage: Int, action: (File) -> Unit) {
        if (currentPage < pictures.size) {
            action(pictures[currentPage])
        }
    }

    fun deleteImage(currentPage: Int, action: () -> Unit) {
        if (currentPage < pictures.size) {
            val picture = pictures[currentPage]
            if (picture.exists() && picture.delete()) {
                pictures.removeAt(currentPage)
                notifyItemRemoved(currentPage)
                action()
            }
        }
    }

    class PicturesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagePreview: ImageView = itemView.findViewById(R.id.imagePreview)
    }
}