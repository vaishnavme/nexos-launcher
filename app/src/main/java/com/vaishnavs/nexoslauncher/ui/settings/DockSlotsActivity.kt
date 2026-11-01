package com.vaishnavs.nexoslauncher.ui.settings

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.vaishnavs.nexoslauncher.R
import com.vaishnavs.nexoslauncher.data.apps.AppRepository
import com.vaishnavs.nexoslauncher.data.prefs.DockPrefs
import com.vaishnavs.nexoslauncher.databinding.ActivityDockSlotsBinding
import com.vaishnavs.nexoslauncher.databinding.ItemSettingsRowBinding

class DockSlotsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDockSlotsBinding
    private lateinit var dockPrefs: DockPrefs
    private lateinit var appRepository: AppRepository
    private val slotRows = mutableListOf<ItemSettingsRowBinding>()
    private var editingSlot = 0

    private val pickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val flattened = result.data?.getStringExtra(DockAppPickerActivity.EXTRA_COMPONENT)
        val component = flattened?.let { ComponentName.unflattenFromString(it) }
        dockPrefs.setSlot(editingSlot, component)
        bindSlots()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDockSlotsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dockPrefs = DockPrefs(this)
        appRepository = AppRepository(packageManager, packageName)
        inflateRows()
        bindSlots()
    }

    override fun onResume() {
        super.onResume()
        bindSlots()
    }

    private fun inflateRows() {
        val inflater = LayoutInflater.from(this)
        repeat(DockPrefs.MAX_SLOTS) { index ->
            if (index > 0) {
                binding.slotsContainer.addView(divider())
            }
            val row = ItemSettingsRowBinding.inflate(inflater, binding.slotsContainer, false)
            binding.slotsContainer.addView(row.root)
            slotRows.add(row)
        }
    }

    private fun divider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.settings_divider_height).takeIf { it > 0 }
                    ?: 1
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.settings_divider_inset)
            }
            setBackgroundResource(R.drawable.bg_settings_divider)
        }
    }

    private fun bindSlots() {
        val slots = dockPrefs.loadSlots()
        slotRows.forEachIndexed { index, row ->
            bindRow(row, index, slots.getOrNull(index))
        }
    }

    private fun bindRow(row: ItemSettingsRowBinding, index: Int, component: ComponentName?) {
        row.rowCaption.text = getString(R.string.settings_slot, index + 1)
        val app = component?.let { appRepository.loadApp(it) }
        if (app == null) {
            row.rowTitle.text = getString(R.string.settings_none)
            row.rowIcon.setImageDrawable(null)
            row.rowIcon.setBackgroundResource(R.drawable.bg_empty_slot)
        } else {
            row.rowTitle.text = app.label
            row.rowIcon.background = null
            row.rowIcon.setImageDrawable(appRepository.loadIcon(app))
        }
        row.root.setOnClickListener { openPicker(index, component) }
    }

    private fun openPicker(index: Int, current: ComponentName?) {
        editingSlot = index
        val intent = Intent(this, DockAppPickerActivity::class.java)
            .putExtra(DockAppPickerActivity.EXTRA_SLOT, index)
            .putExtra(DockAppPickerActivity.EXTRA_COMPONENT, current?.flattenToString())
        pickerLauncher.launch(intent)
    }
}
