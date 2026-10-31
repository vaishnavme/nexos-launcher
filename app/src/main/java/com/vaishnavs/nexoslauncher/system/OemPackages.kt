package com.vaishnavs.nexoslauncher.system

/**
 * Probe lists of stock head-unit packages. Replace with the real packages from
 * the on-device ADB dump in docs/adb-discovery.md before wiring tuner or EQ.
 */
object OemPackages {
    val radio = listOf(
        "com.tw.radio",
        "com.android.fmradio",
        "com.android.fmradio.ext",
        "com.syu.radio",
        "com.acloud.stub.localradio",
        "com.microntek.radio",
        "com.szchoiceway.radio"
    )

    val eq = listOf(
        "com.tw.eq",
        "com.syu.eq",
        "com.android.soundprocessor",
        "com.microntek.dsp",
        "com.szchoiceway.eq"
    )

    val bluetooth = listOf(
        "com.tw.bt",
        "com.syu.bt",
        "com.android.bluetooth",
        "com.microntek.bluetooth",
        "com.szchoiceway.btsuite"
    )

    val music = listOf(
        "com.tw.music",
        "com.syu.music",
        "com.acloud.stub.localmusic",
        "com.microntek.music",
        "com.android.music"
    )

    val maps = listOf(
        "com.google.android.apps.maps",
        "com.waze",
        "com.sygic.aura",
        "com.nng.igo.primong",
        "com.nng.igoprimo"
    )

    val radioLabels = listOf("radio", "fm")
    val eqLabels = listOf("eq", "dsp", "equalizer", "sound")
    val musicLabels = listOf("music", "audio player")
    val phoneLabels = listOf("bluetooth", "phone", "bt")
    val mapsLabels = listOf("maps", "map", "navigation", "navi")
}
