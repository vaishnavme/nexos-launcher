package com.vaishnavs.nexoslauncher.data.apps

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

class AppRepository(
    private val packageManager: PackageManager,
    private val selfPackageName: String,
    private val iconCache: IconCache = IconCache()
) {
    fun loadLauncherApps(): List<AppInfo> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(query, 0)
            .asSequence()
            .map { resolveInfo ->
                AppInfo(
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    activityName = resolveInfo.activityInfo.name
                )
            }
            .filter { it.packageName != selfPackageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun loadIcon(app: AppInfo): Drawable {
        val key = "${app.packageName}/${app.activityName}"
        return iconCache.get(key) {
            packageManager.getActivityIcon(app.componentName)
        }
    }
}
