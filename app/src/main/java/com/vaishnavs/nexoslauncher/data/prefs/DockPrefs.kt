package com.vaishnavs.nexoslauncher.data.prefs

import android.content.ComponentName
import android.content.Context
import com.vaishnavs.nexoslauncher.system.OemAppResolver
import org.json.JSONArray

class DockPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSlots(): List<ComponentName?> {
        val json = JSONArray(prefs.getString(KEY_SLOTS, emptySlotsJson()))
        return (0 until MAX_SLOTS).map { index ->
            val value = json.optString(index, "")
            if (value.isBlank()) null else ComponentName.unflattenFromString(value)
        }
    }

    fun setSlot(index: Int, component: ComponentName?) {
        require(index in 0 until MAX_SLOTS)
        val json = JSONArray(prefs.getString(KEY_SLOTS, emptySlotsJson()))
        while (json.length() < MAX_SLOTS) {
            json.put("")
        }
        json.put(index, component?.flattenToString().orEmpty())
        prefs.edit().putString(KEY_SLOTS, json.toString()).apply()
    }

    fun ensureDefaults(resolver: OemAppResolver) {
        if (prefs.getBoolean(KEY_INITIALIZED, false)) return
        val defaults = listOf(
            resolver.resolvePhone(),
            resolver.resolveMusic(),
            resolver.resolveMaps(),
            resolver.resolveRadio()
        )
        val json = JSONArray()
        repeat(MAX_SLOTS) { index ->
            json.put(defaults.getOrNull(index)?.flattenToString().orEmpty())
        }
        prefs.edit()
            .putString(KEY_SLOTS, json.toString())
            .putBoolean(KEY_INITIALIZED, true)
            .apply()
    }

    private fun emptySlotsJson(): String {
        val json = JSONArray()
        repeat(MAX_SLOTS) { json.put("") }
        return json.toString()
    }

    companion object {
        const val MAX_SLOTS = 6
        private const val PREFS_NAME = "dock"
        private const val KEY_SLOTS = "slots"
        private const val KEY_INITIALIZED = "initialized"
    }
}
