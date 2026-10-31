package com.vaishnavs.nexoslauncher.system

import android.content.Context
import android.util.Log
import com.vaishnavs.nexoslauncher.NexOSApp

class OemLaunchRadioGateway(
    private val oemAppResolver: OemAppResolver
) : RadioGateway {
    override fun openRadio(context: Context): Boolean {
        if (oemAppResolver.launchFirst(context, OemPackages.radio)) return true
        return oemAppResolver.launchByLabelContains(context, OemPackages.radioLabels)
    }

    override fun tune(frequencyKhz: Int): Boolean {
        Log.i(NexOSApp.TAG, "tune($frequencyKhz) unsupported until OEM radio intents are known")
        return false
    }

    override fun seek(direction: SeekDirection): Boolean {
        Log.i(NexOSApp.TAG, "seek($direction) unsupported until OEM radio intents are known")
        return false
    }

    override fun presets(): List<RadioPreset> = emptyList()
}
