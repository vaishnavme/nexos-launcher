package com.vaishnavs.nexoslauncher.data.apps

import android.content.ComponentName

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String
) {
    val componentName: ComponentName
        get() = ComponentName(packageName, activityName)
}
