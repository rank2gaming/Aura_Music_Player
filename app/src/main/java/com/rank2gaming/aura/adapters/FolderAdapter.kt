package com.rank2gaming.aura.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rank2gaming.aura.databinding.ItemFolderBinding
import java.io.File

data class FolderData(val path: String, val songCount: Int)

class FolderAdapter(
    private var folders: List<FolderData>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount() = folders.size

    inner class FolderViewHolder(private val binding: ItemFolderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(folder: FolderData) {
            val file = File(folder.path)
            binding.txtFolderName.text = file.name
            binding.txtFolderCount.text = "${folder.songCount} Songs"
            binding.root.setOnClickListener { onClick(folder.path) }
        }
    }
}