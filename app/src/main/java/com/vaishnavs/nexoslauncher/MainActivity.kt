package com.vaishnavs.nexoslauncher

import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commit
import com.vaishnavs.nexoslauncher.databinding.ActivityMainBinding
import com.vaishnavs.nexoslauncher.system.AndroidAudioGateway
import com.vaishnavs.nexoslauncher.system.AudioGateway
import com.vaishnavs.nexoslauncher.system.OemAppResolver
import com.vaishnavs.nexoslauncher.system.OemLaunchRadioGateway
import com.vaishnavs.nexoslauncher.system.RadioGateway
import com.vaishnavs.nexoslauncher.system.StatusController
import com.vaishnavs.nexoslauncher.ui.apps.AppsFragment
import com.vaishnavs.nexoslauncher.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var oemAppResolver: OemAppResolver
    private lateinit var radioGateway: RadioGateway
    private lateinit var audioGateway: AudioGateway
    private var statusController: StatusController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        oemAppResolver = OemAppResolver(packageManager)
        radioGateway = OemLaunchRadioGateway(oemAppResolver)
        audioGateway = AndroidAudioGateway(
            audioManager = checkNotNull(getSystemService(AudioManager::class.java)),
            oemAppResolver = oemAppResolver
        )
        Log.i(
            NexOSApp.TAG,
            "Media volume ${audioGateway.getMediaVolume()}/${audioGateway.getMaxMediaVolume()}"
        )

        statusController = StatusController(
            context = this,
            clockView = binding.clock,
            bluetoothView = binding.statusBluetooth,
            wifiView = binding.statusWifi
        ).also { lifecycle.addObserver(it) }

        binding.dockPhone.setOnClickListener { openPhone() }
        binding.dockMusic.setOnClickListener { openMusic() }
        binding.dockMusic.setOnLongClickListener { openEq() }
        binding.dockMaps.setOnClickListener { openMaps() }
        binding.dockRadio.setOnClickListener { openRadio() }
        binding.dockApps.setOnClickListener { showApps() }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (supportFragmentManager.findFragmentById(R.id.content_container) is AppsFragment) {
                        showHome()
                    }
                }
            }
        )

        updateDockSelection()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun openPhone() {
        if (!oemAppResolver.launchDialer(this)) {
            toast(R.string.phone_not_found)
        }
    }

    private fun openMusic() {
        if (!oemAppResolver.launchMusic(this)) {
            toast(R.string.music_not_found)
        }
    }

    private fun openMaps() {
        if (!oemAppResolver.launchMaps(this)) {
            toast(R.string.maps_not_found)
        }
    }

    private fun openRadio() {
        if (!radioGateway.openRadio(this)) {
            toast(R.string.radio_not_found)
        }
    }

    private fun openEq(): Boolean {
        if (!audioGateway.openEq(this)) {
            toast(R.string.eq_not_found)
        }
        return true
    }

    private fun showApps() {
        if (supportFragmentManager.findFragmentById(R.id.content_container) is AppsFragment) {
            return
        }
        supportFragmentManager.commit {
            replace(R.id.content_container, AppsFragment())
        }
        updateDockSelection(appsSelected = true)
    }

    private fun showHome() {
        supportFragmentManager.commit {
            replace(R.id.content_container, HomeFragment())
        }
        updateDockSelection(appsSelected = false)
    }

    private fun updateDockSelection(appsSelected: Boolean? = null) {
        val selected = appsSelected
            ?: (supportFragmentManager.findFragmentById(R.id.content_container) is AppsFragment)
        setDockSelected(binding.dockPhone, false)
        setDockSelected(binding.dockMusic, false)
        setDockSelected(binding.dockMaps, false)
        setDockSelected(binding.dockRadio, false)
        setDockSelected(binding.dockApps, selected)
    }

    private fun setDockSelected(view: View, selected: Boolean) {
        view.isSelected = selected
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }
}
