package com.vaishnavs.nexoslauncher.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.vaishnavs.nexoslauncher.ui.settings.SettingsActivity

object AppIntents {
    fun start(context: Context, component: ComponentName) {
        if (component.packageName == context.packageName &&
            component.className == SettingsActivity::class.java.name
        ) {
            context.startActivity(
                Intent(context, SettingsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
        context.startActivity(intent)
    }
}
