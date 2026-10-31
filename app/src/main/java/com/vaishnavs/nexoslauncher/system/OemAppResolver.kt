package com.vaishnavs.nexoslauncher.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.vaishnavs.nexoslauncher.NexOSApp

/**
 * Resolves and launches stock OEM apps by package name, then by launcher label.
 * Does not talk to MCU, QN8035, or AK7604.
 */
class OemAppResolver(
    private val packageManager: PackageManager
) {
    fun findInstalled(candidates: List<String>): String? {
        return candidates.firstOrNull { packageName ->
            try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun launchFirst(context: Context, candidates: List<String>): Boolean {
        val packageName = findInstalled(candidates) ?: return false
        return launchPackage(context, packageName)
    }

    fun launchPackage(context: Context, packageName: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(context, intent)
    }

    fun launchByLabelContains(context: Context, keywords: List<String>): Boolean {
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = packageManager.queryIntentActivities(launchIntent, 0)
        val match = matches.firstOrNull { resolveInfo ->
            val label = resolveInfo.loadLabel(packageManager).toString().lowercase()
            keywords.any { keyword -> label.contains(keyword) }
        } ?: return false
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(
                match.activityInfo.packageName,
                match.activityInfo.name
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(context, intent)
    }

    fun launchDialer(context: Context): Boolean {
        if (launchFirst(context, OemPackages.bluetooth)) return true
        if (launchByLabelContains(context, OemPackages.phoneLabels)) return true
        val dial = Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(context, dial)
    }

    fun launchMaps(context: Context): Boolean {
        if (launchFirst(context, OemPackages.maps)) return true
        if (launchByLabelContains(context, OemPackages.mapsLabels)) return true
        val selector = Intent.makeMainSelectorActivity(
            Intent.ACTION_MAIN,
            Intent.CATEGORY_APP_MAPS
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (startSafely(context, selector)) return true
        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(context, geo)
    }

    fun launchMusic(context: Context): Boolean {
        if (launchFirst(context, OemPackages.music)) return true
        if (launchByLabelContains(context, OemPackages.musicLabels)) return true
        val selector = Intent.makeMainSelectorActivity(
            Intent.ACTION_MAIN,
            Intent.CATEGORY_APP_MUSIC
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(context, selector)
    }

    private fun startSafely(context: Context, intent: Intent): Boolean {
        return try {
            if (intent.resolveActivity(packageManager) == null &&
                intent.component == null &&
                intent.getPackage() == null
            ) {
                false
            } else {
                context.startActivity(intent)
                true
            }
        } catch (error: Exception) {
            Log.w(NexOSApp.TAG, "Failed to launch ${intent.component ?: intent.action}", error)
            false
        }
    }
}
