package com.vaishnavs.nexoslauncher.ui.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vaishnavs.nexoslauncher.data.apps.AppInfo
import com.vaishnavs.nexoslauncher.data.apps.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(
        packageManager = application.packageManager,
        selfPackageName = application.packageName
    )

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.Default) {
                repository.loadLauncherApps()
            }
        }
    }

    fun iconFor(app: AppInfo) = repository.loadIcon(app)

    fun topMatches(query: String): List<AppInfo> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return apps.value
            .filter { it.label.contains(trimmed, ignoreCase = true) }
            .take(3)
    }
}
