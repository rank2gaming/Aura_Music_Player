package com.rank2gaming.aura.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rank2gaming.aura.R
import com.rank2gaming.aura.databinding.ItemManagePlaylistBinding
import com.rank2gaming.aura.utils.PlaylistCoverManager

data class ManageData(val name: String, val count: Int)

class ManagePlaylistAdapter(
    private var list: List<ManageData>,
    private val onCoverClick: (String) -> Unit,
    private val onRenameClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<ManagePlaylistAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemManagePlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size

    inner class ViewHolder(private val binding: ItemManagePlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ManageData) {
            binding.txtName.text = item.name
            binding.txtInfo.text = "${item.count} Songs"

            // Load Custom Cover
            val customCover = PlaylistCoverManager.getCover(binding.root.context, item.name)
            if (customCover != null) {
                Glide.with(binding.root).load(Uri.parse(customCover)).circleCrop().into(binding.imgPlaylistCover)
            } else {
                binding.imgPlaylistCover.setImageResource(R.drawable.ic_music_note)
            }

            // Click Listeners
            binding.cardCover.setOnClickListener { onCoverClick(item.name) }
            binding.btnRename.setOnClickListener { onRenameClick(item.name) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item.name) }
        }
    }
}