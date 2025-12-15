package com.rank2gaming.aura.youtube.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rank2gaming.aura.youtube.databinding.ItemYoutubeVideoBinding

class VideoAdapter(
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    private val videos = ArrayList<YouTubeVideoItem>()

    fun setList(newList: List<YouTubeVideoItem>) {
        videos.clear()
        videos.addAll(newList)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemYoutubeVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemYoutubeVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = videos[position]

        holder.binding.txtTitle.text = item.snippet.title
        holder.binding.txtChannel.text = item.snippet.channelTitle

        Glide.with(holder.itemView.context)
            .load(item.snippet.thumbnails.medium.url)
            .into(holder.binding.imgThumbnail) // XML handles scaleType

        holder.itemView.setOnClickListener {
            onClick(item.id)
        }
    }

    override fun getItemCount() = videos.size
}