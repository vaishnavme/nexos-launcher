package com.vaishnavs.nexoslauncher.ui.apps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vaishnavs.nexoslauncher.data.apps.AppInfo
import com.vaishnavs.nexoslauncher.databinding.ItemAppBinding

class AppGridAdapter(
    private val iconProvider: (AppInfo) -> android.graphics.drawable.Drawable,
    private val onAppClicked: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppGridAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemAppBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppInfo) {
            binding.appLabel.text = app.label
            binding.appIcon.setImageDrawable(iconProvider(app))
            binding.root.setOnClickListener(null)
            binding.root.isClickable = false
            binding.appHitTarget.setOnClickListener { onAppClicked(app) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName &&
                oldItem.activityName == newItem.activityName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}
