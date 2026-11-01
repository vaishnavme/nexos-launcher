package com.vaishnavs.nexoslauncher.system

import android.content.Context
import android.media.AudioManager
import kotlin.math.max
import kotlin.math.min

class AndroidAudioGateway(
    private val audioManager: AudioManager,
    private val oemAppResolver: OemAppResolver
) : AudioGateway {
    override fun getMediaVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    override fun getMaxMediaVolume(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    override fun setMediaVolume(level: Int) {
        val max = getMaxMediaVolume()
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            min(max, max(0, level)),
            0
        )
    }

    override fun raiseVolume() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            0
        )
    }

    override fun lowerVolume() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            0
        )
    }

    override fun openEq(context: Context): Boolean {
        if (oemAppResolver.launchFirst(context, OemPackages.eq)) return true
        return oemAppResolver.launchByLabelContains(context, OemPackages.eqLabels)
    }
}
