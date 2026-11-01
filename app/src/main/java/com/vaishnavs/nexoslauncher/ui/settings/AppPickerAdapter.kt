package com.vaishnavs.nexoslauncher.ui.settings

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.vaishnavs.nexoslauncher.R
import com.vaishnavs.nexoslauncher.data.apps.AppInfo
import com.vaishnavs.nexoslauncher.databinding.ItemAppPickerBinding

class AppPickerAdapter(
    private val iconProvider: (AppInfo) -> Drawable,
    private val selectedComponent: ComponentName?,
    private val onAppClicked: (AppInfo?) -> Unit
) : RecyclerView.Adapter<AppPickerAdapter.Holder>() {
    private val items = mutableListOf<AppInfo?>()

    fun submit(apps: List<AppInfo>) {
        items.clear()
        items.add(null)
        items.addAll(apps)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemAppPickerBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(
        private val binding: ItemAppPickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppInfo?) {
            if (app == null) {
                binding.pickerLabel.setText(R.string.settings_none)
                binding.pickerIcon.setImageDrawable(null)
                binding.pickerIcon.setBackgroundResource(R.drawable.bg_empty_slot)
                binding.pickerCheck.isVisible = selectedComponent == null
            } else {
                binding.pickerLabel.text = app.label
                binding.pickerIcon.background = null
                binding.pickerIcon.setImageDrawable(iconProvider(app))
                binding.pickerCheck.isVisible = app.componentName == selectedComponent
            }
            binding.root.setOnClickListener { onAppClicked(app) }
        }
    }
}
