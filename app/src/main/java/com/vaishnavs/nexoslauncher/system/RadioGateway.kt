package com.vaishnavs.nexoslauncher.system

import android.content.Context

data class RadioPreset(
    val index: Int,
    val frequencyKhz: Int,
    val name: String? = null
)

enum class SeekDirection {
    UP,
    DOWN
}

/**
 * FM radio access for the QN8035. Until OEM intents are known, only
 * [openRadio] is implemented (launches the factory Radio app).
 */
interface RadioGateway {
    fun openRadio(context: Context): Boolean
    fun tune(frequencyKhz: Int): Boolean
    fun seek(direction: SeekDirection): Boolean
    fun presets(): List<RadioPreset>
}
