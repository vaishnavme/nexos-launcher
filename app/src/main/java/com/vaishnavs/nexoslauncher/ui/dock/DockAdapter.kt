package com.vaishnavs.nexoslauncher.ui.dock

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaishnavs.nexoslauncher.databinding.ItemDockBinding

class DockAdapter(
    private val iconProvider: (ComponentName) -> Drawable?,
    private val onAppClicked: (ComponentName) -> Unit
) : RecyclerView.Adapter<DockAdapter.Holder>() {
    private val items = mutableListOf<ComponentName>()

    fun submit(slots: List<ComponentName?>) {
        items.clear()
        items.addAll(slots.filterNotNull())
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemDockBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(
        private val binding: ItemDockBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(component: ComponentName) {
            binding.dockIcon.setImageDrawable(iconProvider(component))
            binding.root.setOnClickListener { onAppClicked(component) }
        }
    }
}
