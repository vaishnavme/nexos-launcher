package com.vaishnavs.nexoslauncher.data.apps

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.vaishnavs.nexoslauncher.MainActivity

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
            .filterNot { it.packageName == selfPackageName && it.activityName == MainActivity::class.java.name }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun loadApp(componentName: ComponentName): AppInfo? {
        return runCatching {
            val info = packageManager.getActivityInfo(componentName, 0)
            AppInfo(
                label = info.loadLabel(packageManager).toString(),
                packageName = componentName.packageName,
                activityName = componentName.className
            )
        }.getOrNull()
    }

    fun loadIcon(app: AppInfo): Drawable = loadIcon(app.componentName)

    fun loadIcon(componentName: ComponentName): Drawable {
        val key = componentName.flattenToString()
        return iconCache.get(key) {
            SquircleIcons.flatten(packageManager.getActivityIcon(componentName))
        }
    }
}
