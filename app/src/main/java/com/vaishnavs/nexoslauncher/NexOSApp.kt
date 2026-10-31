package com.vaishnavs.nexoslauncher

import android.app.Application
import android.util.Log

class NexOSApp : Application() {
    override fun onCreate() {
        super.onCreate()
        logDisplayMetrics()
    }

    private fun logDisplayMetrics() {
        val metrics = resources.displayMetrics
        Log.i(
            TAG,
            "Display ${metrics.widthPixels}x${metrics.heightPixels} " +
                "density=${metrics.density} densityDpi=${metrics.densityDpi} " +
                "xdpi=${metrics.xdpi} ydpi=${metrics.ydpi}"
        )
    }

    companion object {
        const val TAG = "NexOS"
    }
}
