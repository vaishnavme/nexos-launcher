package com.vaishnavs.nexoslauncher.ui.home

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.vaishnavs.nexoslauncher.R
import com.vaishnavs.nexoslauncher.databinding.FragmentHomeBinding
import com.vaishnavs.nexoslauncher.databinding.ItemHomeTileBinding
import com.vaishnavs.nexoslauncher.system.AndroidAudioGateway
import com.vaishnavs.nexoslauncher.system.AudioGateway
import com.vaishnavs.nexoslauncher.system.HomeStatus
import com.vaishnavs.nexoslauncher.system.OemAppResolver
import com.vaishnavs.nexoslauncher.system.StatusController
import com.vaishnavs.nexoslauncher.ui.widget.SpeedArcView

class HomeFragment : Fragment() {
    private var binding: FragmentHomeBinding? = null
    private var statusController: StatusController? = null
    private lateinit var oemAppResolver: OemAppResolver
    private lateinit var audioGateway: AudioGateway
    private var tlinkInstalled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentHomeBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val home = binding ?: return
        oemAppResolver = OemAppResolver(requireContext().packageManager)
        audioGateway = AndroidAudioGateway(
            requireContext().getSystemService(AudioManager::class.java),
            oemAppResolver
        )
        tlinkInstalled = oemAppResolver.resolveTlink() != null

        home.speedArc.speed = SpeedArcView.PLACEHOLDER_SPEED
        home.speedValue.text = SpeedArcView.PLACEHOLDER_SPEED.toString()

        bindTile(
            tile = home.tileWifi,
            icon = R.drawable.ic_wifi,
            name = R.string.tile_wifi,
            onClick = ::toggleWifi,
            onLongClick = {
                openSystemSettings(Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS)
            }
        )
        bindTile(
            tile = home.tileBluetooth,
            icon = R.drawable.ic_bluetooth,
            name = R.string.tile_bluetooth,
            onClick = ::toggleBluetooth,
            onLongClick = { openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS) }
        )
        bindTile(
            tile = home.tileTlink,
            icon = R.drawable.ic_tlink,
            name = R.string.tile_tlink,
            status = if (tlinkInstalled) getString(R.string.status_on) else getString(R.string.status_not_installed),
            onClick = ::openTlink
        )
        bindTile(
            tile = home.tileStereo,
            icon = R.drawable.ic_eq,
            name = R.string.tile_stereo,
            status = getString(R.string.status_on),
            onClick = ::openStereo
        )

        val controller = StatusController(requireContext(), ::onStatus)
        statusController = controller
        viewLifecycleOwner.lifecycle.addObserver(controller)
    }

    override fun onDestroyView() {
        statusController?.let { viewLifecycleOwner.lifecycle.removeObserver(it) }
        statusController = null
        binding = null
        super.onDestroyView()
    }

    private fun onStatus(status: HomeStatus) {
        val home = binding ?: return
        home.homeTime.text = status.timeText
        home.homeDate.text = status.dateText
        applyRadioState(
            tile = home.tileWifi,
            enabled = status.wifiEnabled,
            connected = status.wifiConnected
        )
        applyRadioState(
            tile = home.tileBluetooth,
            enabled = status.bluetoothEnabled,
            connected = status.bluetoothConnected
        )
    }

    private fun bindTile(
        tile: ItemHomeTileBinding,
        icon: Int,
        name: Int,
        status: String? = null,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
    ) {
        tile.tileIcon.setImageResource(icon)
        tile.tileName.setText(name)
        if (status != null) {
            tile.tileStatus.text = status
        }
        tile.root.setOnClickListener { onClick() }
        tile.root.isLongClickable = onLongClick != null
        tile.root.setOnLongClickListener(
            if (onLongClick == null) {
                null
            } else {
                View.OnLongClickListener {
                    onLongClick()
                    true
                }
            }
        )
    }

    private fun applyRadioState(
        tile: ItemHomeTileBinding,
        enabled: Boolean,
        connected: Boolean
    ) {
        val iconColor: Int
        val background: Int
        val statusText: String
        when {
            connected -> {
                iconColor = R.color.tile_icon_connected
                background = R.drawable.bg_home_tile_connected
                statusText = getString(R.string.status_connected)
            }
            enabled -> {
                iconColor = R.color.tile_icon_on
                background = R.drawable.bg_home_tile_on
                statusText = getString(R.string.status_on)
            }
            else -> {
                iconColor = R.color.tile_icon_off
                background = R.drawable.bg_home_tile
                statusText = getString(R.string.status_off)
            }
        }
        tile.root.setBackgroundResource(background)
        ImageViewCompat.setImageTintList(
            tile.tileIcon,
            ContextCompat.getColorStateList(requireContext(), iconColor)
        )
        tile.tileStatus.text = statusText
        tile.tileName.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (enabled) R.color.white else R.color.grey
            )
        )
    }

    private fun toggleWifi() {
        val toggled = statusController?.toggleWifi() == true
        if (!toggled) {
            openSystemSettings(Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS)
        }
    }

    private fun toggleBluetooth() {
        val toggled = statusController?.toggleBluetooth() == true
        if (!toggled) {
            openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
        }
    }

    private fun openSystemSettings(vararg actions: String) {
        for (action in actions) {
            val launched = runCatching {
                startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (launched) return
        }
    }

    private fun openTlink() {
        if (oemAppResolver.launchTlink(requireContext())) return
        Toast.makeText(requireContext(), R.string.tlink_not_found, Toast.LENGTH_SHORT).show()
    }

    private fun openStereo() {
        if (audioGateway.openEq(requireContext())) return
        if (oemAppResolver.launchMusic(requireContext())) return
        Toast.makeText(requireContext(), R.string.eq_not_found, Toast.LENGTH_SHORT).show()
    }
}
