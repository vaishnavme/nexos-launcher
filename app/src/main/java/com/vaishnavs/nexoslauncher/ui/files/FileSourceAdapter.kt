package com.vaishnavs.nexoslauncher.ui.files

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vaishnavs.nexoslauncher.R
import com.vaishnavs.nexoslauncher.data.files.FileSource
import com.vaishnavs.nexoslauncher.data.files.MediaCategory
import com.vaishnavs.nexoslauncher.data.files.VolumeKind
import com.vaishnavs.nexoslauncher.databinding.ItemFileSourceBinding

data class FileSourceRow(
    val source: FileSource,
    val selected: Boolean
)

class FileSourceAdapter(
    private val onSourceClicked: (FileSource) -> Unit
) : ListAdapter<FileSourceRow, FileSourceAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemFileSourceBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(
        private val binding: ItemFileSourceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: FileSourceRow) {
            binding.sourceLabel.text = row.source.label
            binding.sourceIcon.setImageResource(iconRes(row.source))
            binding.root.isSelected = row.selected
            binding.root.setOnClickListener { onSourceClicked(row.source) }
        }
    }

    private fun iconRes(source: FileSource): Int {
        return when (source) {
            is FileSource.Volume -> when (source.kind) {
                VolumeKind.INTERNAL -> R.drawable.ic_storage
                VolumeKind.SD -> R.drawable.ic_sd
                VolumeKind.USB -> R.drawable.ic_usb
            }
            is FileSource.Media -> when (source.category) {
                MediaCategory.IMAGES -> R.drawable.ic_image
                MediaCategory.AUDIO -> R.drawable.ic_music
                MediaCategory.VIDEO -> R.drawable.ic_video
                MediaCategory.DOCUMENTS -> R.drawable.ic_document
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<FileSourceRow>() {
        override fun areItemsTheSame(oldItem: FileSourceRow, newItem: FileSourceRow): Boolean {
            return oldItem.source.id == newItem.source.id
        }

        override fun areContentsTheSame(oldItem: FileSourceRow, newItem: FileSourceRow): Boolean {
            return oldItem == newItem
        }
    }
}
