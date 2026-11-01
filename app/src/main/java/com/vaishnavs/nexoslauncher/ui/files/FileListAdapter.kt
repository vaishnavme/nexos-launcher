package com.vaishnavs.nexoslauncher.ui.files

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vaishnavs.nexoslauncher.data.files.FileEntry
import com.vaishnavs.nexoslauncher.databinding.ItemFileBinding

data class FileRow(
    val entry: FileEntry,
    val selected: Boolean
)

class FileListAdapter(
    private val thumbnailLoader: ThumbnailLoader,
    private val onFileClicked: (FileEntry) -> Unit,
    private val onFileLongClicked: (FileEntry) -> Boolean
) : ListAdapter<FileRow, FileListAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemFileBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(
        private val binding: ItemFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val iconPadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8f,
            binding.root.resources.displayMetrics
        ).toInt()

        fun bind(row: FileRow) {
            val entry = row.entry
            binding.fileName.text = entry.name
            binding.fileMeta.text = FileFormats.meta(entry)
            binding.selectedMark.isVisible = row.selected
            binding.fileIcon.tag = null
            binding.fileIcon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            binding.fileIcon.setImageResource(FileFormats.iconRes(entry))
            thumbnailLoader.load(entry, binding.fileIcon)
            binding.root.setOnClickListener { onFileClicked(entry) }
            binding.root.setOnLongClickListener { onFileLongClicked(entry) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<FileRow>() {
        override fun areItemsTheSame(oldItem: FileRow, newItem: FileRow): Boolean {
            return oldItem.entry.path == newItem.entry.path
        }

        override fun areContentsTheSame(oldItem: FileRow, newItem: FileRow): Boolean {
            return oldItem == newItem
        }
    }
}
