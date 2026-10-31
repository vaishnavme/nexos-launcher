package com.vaishnavs.nexoslauncher.system

import android.bluetooth.BluetoothAdapter
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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.vaishnavs.nexoslauncher.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatusController(
    private val context: Context,
    private val clockView: TextView,
    private val bluetoothView: ImageView,
    private val wifiView: ImageView
) : DefaultLifecycleObserver {
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? =
        context.applicationContext.getSystemService(BluetoothManager::class.java)?.adapter

    private val clockTick = object : Runnable {
        override fun run() {
            clockView.text = timeFormat.format(Date())
            handler.postDelayed(this, CLOCK_INTERVAL_MS)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateIndicators()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            handler.post { updateWifi() }
        }

        override fun onLost(network: Network) {
            handler.post { updateWifi() }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            handler.post { updateWifi() }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        clockTick.run()
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
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
        updateIndicators()
    }

    override fun onStop(owner: LifecycleOwner) {
        handler.removeCallbacks(clockTick)
        runCatching { context.unregisterReceiver(receiver) }
        runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
    }

    private fun updateIndicators() {
        updateBluetooth()
        updateWifi()
    }

    private fun updateBluetooth() {
        val adapter = bluetoothAdapter
        val enabled = adapter?.isEnabled == true
        val connected = enabled && isBluetoothConnected(adapter)
        bluetoothView.alpha = when {
            connected -> 1f
            enabled -> 0.7f
            else -> 0.35f
        }
        bluetoothView.setImageResource(R.drawable.ic_bluetooth)
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

    private fun updateWifi() {
        val wifiEnabled = wifiManager?.isWifiEnabled == true
        val connected = wifiEnabled && isWifiConnected()
        wifiView.alpha = when {
            connected -> 1f
            wifiEnabled -> 0.7f
            else -> 0.35f
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
