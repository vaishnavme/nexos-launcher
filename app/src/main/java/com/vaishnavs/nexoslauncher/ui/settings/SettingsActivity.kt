package com.vaishnavs.nexoslauncher.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vaishnavs.nexoslauncher.R
import com.vaishnavs.nexoslauncher.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.leftDockRow.rowCaption.visibility = View.GONE
        binding.leftDockRow.rowTitle.setText(R.string.settings_left_dock)
        binding.leftDockRow.rowIcon.setImageResource(R.drawable.ic_apps)
        binding.leftDockRow.rowIcon.background = null
        binding.leftDockRow.root.setOnClickListener {
            startActivity(Intent(this, DockSlotsActivity::class.java))
        }
    }
}
