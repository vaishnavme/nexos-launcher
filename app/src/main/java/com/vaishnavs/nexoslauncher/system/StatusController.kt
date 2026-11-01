package com.vaishnavs.nexoslauncher.system

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HomeStatus(
    val timeText: String,
    val dateText: String,
    val wifiEnabled: Boolean,
    val wifiConnected: Boolean,
    val bluetoothEnabled: Boolean,
    val bluetoothConnected: Boolean
)

class StatusController(
    private val context: Context,
    private val onStatus: (HomeStatus) -> Unit
) : DefaultLifecycleObserver {
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    private val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? =
        context.applicationContext.getSystemService(BluetoothManager::class.java)?.adapter

    private val clockTick = object : Runnable {
        override fun run() {
            publish()
            handler.postDelayed(this, CLOCK_INTERVAL_MS)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            publish()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            handler.post { publish() }
        }

        override fun onLost(network: Network) {
            handler.post { publish() }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            handler.post { publish() }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        clockTick.run()
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        publish()
    }

    override fun onStop(owner: LifecycleOwner) {
        handler.removeCallbacks(clockTick)
        runCatching { context.unregisterReceiver(receiver) }
        runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
    }

    @Suppress("DEPRECATION")
    fun toggleWifi(): Boolean {
        val wifi = wifiManager ?: return false
        val enabled = runCatching { wifi.isWifiEnabled }.getOrDefault(false)
        return runCatching { wifi.setWifiEnabled(!enabled) }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    fun toggleBluetooth(): Boolean {
        val adapter = bluetoothAdapter ?: return false
        val enabled = runCatching { adapter.isEnabled }.getOrDefault(false)
        return runCatching {
            if (enabled) adapter.disable() else adapter.enable()
        }.getOrDefault(false)
    }

    private fun publish() {
        val now = Date()
        val wifiEnabled = runCatching { wifiManager?.isWifiEnabled == true }.getOrDefault(false)
        val bluetoothEnabled = runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false)
        onStatus(
            HomeStatus(
                timeText = timeFormat.format(now),
                dateText = dateFormat.format(now),
                wifiEnabled = wifiEnabled,
                wifiConnected = wifiEnabled && isWifiConnected(),
                bluetoothEnabled = bluetoothEnabled,
                bluetoothConnected = bluetoothEnabled && bluetoothAdapter?.let(::isBluetoothConnected) == true
            )
        )
    }

    private fun isBluetoothConnected(adapter: BluetoothAdapter): Boolean {
        val profiles = intArrayOf(
            BluetoothProfile.HEADSET,
            BluetoothProfile.A2DP
        )
        return profiles.any { profile ->
            runCatching {
                adapter.getProfileConnectionState(profile) == BluetoothProfile.STATE_CONNECTED
            }.getOrDefault(false)
        }
    }

    private fun isWifiConnected(): Boolean {
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    companion object {
        private const val CLOCK_INTERVAL_MS = 15_000L
    }
}
