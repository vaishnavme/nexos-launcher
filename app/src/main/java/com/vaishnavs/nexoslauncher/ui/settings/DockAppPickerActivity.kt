package com.vaishnavs.nexoslauncher.ui.settings

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaishnavs.nexoslauncher.data.apps.AppInfo
import com.vaishnavs.nexoslauncher.data.apps.AppRepository
import com.vaishnavs.nexoslauncher.databinding.ActivityAppPickerBinding

class DockAppPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAppPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = AppRepository(packageManager, packageName)
        val selected = intent.getStringExtra(EXTRA_COMPONENT)
            ?.let { ComponentName.unflattenFromString(it) }

        val adapter = AppPickerAdapter(
            iconProvider = repository::loadIcon,
            selectedComponent = selected,
            onAppClicked = ::returnSelection
        )
        binding.appPickerList.layoutManager = LinearLayoutManager(this)
        binding.appPickerList.adapter = adapter
        adapter.submit(repository.loadLauncherApps())
    }

    private fun returnSelection(app: AppInfo?) {
        val data = Intent().putExtra(EXTRA_SLOT, intent.getIntExtra(EXTRA_SLOT, 0))
        if (app != null) {
            data.putExtra(EXTRA_COMPONENT, app.componentName.flattenToString())
        }
        setResult(RESULT_OK, data)
        finish()
    }

    companion object {
        const val EXTRA_SLOT = "slot"
        const val EXTRA_COMPONENT = "component"
    }
}
