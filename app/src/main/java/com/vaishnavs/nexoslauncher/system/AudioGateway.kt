package com.vaishnavs.nexoslauncher.system

import android.content.Context

/**
 * Android-side media volume plus launch of the factory EQ/DSP app.
 * Does not program the AK7604; Android Equalizer will not hit that chip.
 */
interface AudioGateway {
    fun getMediaVolume(): Int
    fun getMaxMediaVolume(): Int
    fun setMediaVolume(level: Int)
    fun openEq(context: Context): Boolean
}
