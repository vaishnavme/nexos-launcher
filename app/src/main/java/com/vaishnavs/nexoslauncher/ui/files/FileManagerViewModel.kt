package com.vaishnavs.nexoslauncher.ui.files

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vaishnavs.nexoslauncher.R
import com.vaishnavs.nexoslauncher.data.files.FileClipboard
import com.vaishnavs.nexoslauncher.data.files.FileEntry
import com.vaishnavs.nexoslauncher.data.files.FileOperations
import com.vaishnavs.nexoslauncher.data.files.FileSource
import com.vaishnavs.nexoslauncher.data.files.FileTypes
import com.vaishnavs.nexoslauncher.data.files.SortMode
import com.vaishnavs.nexoslauncher.data.files.StorageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class FileManagerState(
    val sources: List<FileSource> = emptyList(),
    val selectedSourceId: String? = null,
    val currentPath: String? = null,
    val rootPath: String? = null,
    val pathLabel: String = "",
    val entries: List<FileEntry> = emptyList(),
    val query: String = "",
    val sort: SortMode = SortMode.NAME,
    val selectedPaths: Set<String> = emptySet(),
    val clipboard: FileClipboard? = null,
    val permissionNeeded: Boolean = false,
    val busy: Boolean = false,
    val picker: Boolean = false,
    val allowMultiple: Boolean = false,
    val canGoUp: Boolean = false,
    val canPaste: Boolean = false,
    val isVirtual: Boolean = false
)

sealed class FileManagerEvent {
    data class Message(val resId: Int, val args: List<Any> = emptyList()) : FileManagerEvent()
    data class OpenFile(val entry: FileEntry) : FileManagerEvent()
    data class PickFiles(val paths: List<String>) : FileManagerEvent()
}

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StorageRepository(application)
    private val _state = MutableStateFlow(FileManagerState())
    val state: StateFlow<FileManagerState> = _state
    private val _events = MutableSharedFlow<FileManagerEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<FileManagerEvent> = _events

    private var allEntries: List<FileEntry> = emptyList()
    private var mimeFilters: List<String> = listOf("*/*")
    private var listJob: Job? = null

    fun captureIntent(intent: Intent, force: Boolean = false) {
        if (_state.value.sources.isNotEmpty() && !force) {
            refresh()
            return
        }
        val picker = intent.action in PICKER_ACTIONS
        val extraMimes = intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.toList().orEmpty()
        val type = intent.type
        mimeFilters = (listOfNotNull(type) + extraMimes).ifEmpty { listOf("*/*") }
        _state.update {
            it.copy(
                picker = picker,
                allowMultiple = picker && intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            )
        }
        reloadSources(selectDefault = true)
    }

    fun hasFullAccess(): Boolean = repository.hasFullAccess()

    fun reloadSources(selectDefault: Boolean = false) {
        viewModelScope.launch {
            val sources = withContext(Dispatchers.IO) { repository.listSources() }
            val currentId = _state.value.selectedSourceId
            val selectedId = when {
                selectDefault || currentId == null || sources.none { it.id == currentId } ->
                    repository.defaultSourceId(sources, mimeFilters)
                else -> currentId
            }
            val selected = sources.firstOrNull { it.id == selectedId }
            _state.update { it.copy(sources = sources) }
            if (selected != null) {
                selectSource(selected, keepPath = !selectDefault && selected.id == currentId)
            }
        }
    }

    fun selectSource(source: FileSource, keepPath: Boolean = false) {
        val current = _state.value
        val rootPath = (source as? FileSource.Volume)?.path
        val path = if (keepPath && current.selectedSourceId == source.id) {
            current.currentPath ?: rootPath
        } else {
            rootPath
        }
        _state.update {
            it.copy(
                selectedSourceId = source.id,
                currentPath = path,
                rootPath = rootPath,
                isVirtual = source is FileSource.Media,
                selectedPaths = emptySet(),
                query = if (keepPath) it.query else ""
            )
        }
        refresh()
    }

    fun refresh() {
        val snapshot = _state.value
        val source = snapshot.sources.firstOrNull { it.id == snapshot.selectedSourceId } ?: return
        val needsPermission = when (source) {
            is FileSource.Volume -> !repository.hasFullAccess()
            is FileSource.Media -> !repository.hasMediaAccess()
        }
        if (needsPermission) {
            allEntries = emptyList()
            _state.update {
                it.copy(
                    permissionNeeded = true,
                    entries = emptyList(),
                    pathLabel = source.label,
                    canGoUp = false,
                    canPaste = false,
                    busy = false
                )
            }
            return
        }
        listJob?.cancel()
        listJob = viewModelScope.launch {
            _state.update { it.copy(permissionNeeded = false, busy = true) }
            val listed = withContext(Dispatchers.IO) {
                runCatching {
                    when (source) {
                        is FileSource.Volume -> repository.listDirectory(
                            snapshot.currentPath ?: source.path
                        )
                        is FileSource.Media -> repository.listMedia(source.category)
                    }
                }.getOrElse { emptyList() }
            }
            allEntries = listed.filter { entry ->
                FileTypes.matchesFilter(entry.mimeType, entry.isDirectory, mimeFilters)
            }
            publishEntries(busy = false)
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        publishEntries()
    }

    fun cycleSort() {
        val next = when (_state.value.sort) {
            SortMode.NAME -> SortMode.DATE
            SortMode.DATE -> SortMode.SIZE
            SortMode.SIZE -> SortMode.NAME
        }
        _state.update { it.copy(sort = next) }
        publishEntries()
        val message = when (next) {
            SortMode.NAME -> R.string.files_sort_name
            SortMode.DATE -> R.string.files_sort_date
            SortMode.SIZE -> R.string.files_sort_size
        }
        _events.tryEmit(FileManagerEvent.Message(message))
    }

    fun goUp(): Boolean {
        val snapshot = _state.value
        if (!snapshot.canGoUp) return false
        val current = snapshot.currentPath ?: return false
        val parent = File(current).parent ?: return false
        _state.update {
            it.copy(currentPath = parent, selectedPaths = emptySet(), query = "")
        }
        refresh()
        return true
    }

    fun onEntryClicked(entry: FileEntry) {
        val snapshot = _state.value
        if (snapshot.selectedPaths.isNotEmpty() || (snapshot.picker && snapshot.allowMultiple)) {
            toggleSelection(entry)
            return
        }
        if (entry.isDirectory) {
            _state.update {
                it.copy(currentPath = entry.path, selectedPaths = emptySet(), query = "")
            }
            refresh()
            return
        }
        if (snapshot.picker) {
            _events.tryEmit(FileManagerEvent.PickFiles(listOf(entry.path)))
        } else {
            _events.tryEmit(FileManagerEvent.OpenFile(entry))
        }
    }

    fun onEntryLongClicked(entry: FileEntry) {
        toggleSelection(entry)
    }

    fun clearSelection() {
        if (_state.value.selectedPaths.isEmpty()) return
        _state.update { it.copy(selectedPaths = emptySet()) }
        publishEntries()
    }

    fun copySelection() {
        val paths = _state.value.selectedPaths
        if (paths.isEmpty()) return
        _state.update {
            it.copy(clipboard = FileClipboard(paths.toList(), cut = false), selectedPaths = emptySet())
        }
        publishEntries()
        _events.tryEmit(FileManagerEvent.Message(R.string.files_copied, listOf(paths.size)))
    }

    fun cutSelection() {
        val paths = _state.value.selectedPaths
        if (paths.isEmpty()) return
        _state.update {
            it.copy(clipboard = FileClipboard(paths.toList(), cut = true), selectedPaths = emptySet())
        }
        publishEntries()
    }

    fun paste() {
        val snapshot = _state.value
        val destPath = snapshot.currentPath
        val clipboard = snapshot.clipboard
        if (snapshot.isVirtual || destPath == null || clipboard == null) {
            _events.tryEmit(FileManagerEvent.Message(R.string.files_cannot_paste_here))
            return
        }
        runOp {
            val sources = clipboard.paths.map { File(it) }
            val dest = File(destPath)
            if (clipboard.cut) {
                val count = FileOperations.move(sources, dest)
                _state.update { it.copy(clipboard = null) }
                emit(FileManagerEvent.Message(R.string.files_moved, listOf(count)))
            } else {
                val count = FileOperations.copy(sources, dest)
                emit(FileManagerEvent.Message(R.string.files_copied, listOf(count)))
            }
        }
    }

    fun deleteSelection() {
        val paths = _state.value.selectedPaths
        if (paths.isEmpty()) return
        runOp {
            val count = FileOperations.delete(paths.map { File(it) })
            _state.update { it.copy(selectedPaths = emptySet()) }
            emit(FileManagerEvent.Message(R.string.files_deleted, listOf(count)))
        }
    }

    fun renameSelection(newName: String) {
        val path = _state.value.selectedPaths.singleOrNull() ?: return
        if (newName.trim().isEmpty()) {
            _events.tryEmit(FileManagerEvent.Message(R.string.files_invalid_name))
            return
        }
        runOp {
            FileOperations.rename(File(path), newName)
            _state.update { it.copy(selectedPaths = emptySet()) }
            emit(FileManagerEvent.Message(R.string.files_renamed))
        }
    }

    fun createFolder(name: String) {
        val destPath = _state.value.currentPath
        if (_state.value.isVirtual || destPath == null) {
            _events.tryEmit(FileManagerEvent.Message(R.string.files_cannot_paste_here))
            return
        }
        if (name.trim().isEmpty()) {
            _events.tryEmit(FileManagerEvent.Message(R.string.files_invalid_name))
            return
        }
        runOp {
            FileOperations.mkdir(File(destPath), name)
            emit(FileManagerEvent.Message(R.string.files_folder_created))
        }
    }

    fun confirmPick() {
        val paths = _state.value.selectedPaths.filter { path ->
            val entry = allEntries.firstOrNull { it.path == path }
            entry != null && !entry.isDirectory
        }
        if (paths.isEmpty()) return
        _events.tryEmit(FileManagerEvent.PickFiles(paths))
    }

    fun selectedEntries(): List<FileEntry> {
        val selected = _state.value.selectedPaths
        return allEntries.filter { it.path in selected }
    }

    private fun toggleSelection(entry: FileEntry) {
        val current = _state.value.selectedPaths
        val next = if (entry.path in current) current - entry.path else current + entry.path
        _state.update { it.copy(selectedPaths = next) }
        publishEntries()
    }

    private fun publishEntries(busy: Boolean = _state.value.busy) {
        val snapshot = _state.value
        val query = snapshot.query.trim()
        val filtered = if (query.isEmpty()) {
            allEntries
        } else {
            allEntries.filter { it.name.contains(query, ignoreCase = true) }
        }
        val sorted = sort(filtered, snapshot.sort)
        val source = snapshot.sources.firstOrNull { it.id == snapshot.selectedSourceId }
        val pathLabel = pathLabel(source, snapshot.currentPath)
        val canGoUp = source is FileSource.Volume &&
            snapshot.currentPath != null &&
            snapshot.rootPath != null &&
            snapshot.currentPath != snapshot.rootPath
        val canPaste = !snapshot.picker &&
            !snapshot.isVirtual &&
            snapshot.clipboard != null &&
            snapshot.currentPath != null
        _state.update {
            it.copy(
                entries = sorted,
                pathLabel = pathLabel,
                canGoUp = canGoUp,
                canPaste = canPaste,
                busy = busy
            )
        }
    }

    private fun sort(entries: List<FileEntry>, mode: SortMode): List<FileEntry> {
        val foldersFirst = compareBy<FileEntry> { if (it.isDirectory) 0 else 1 }
        val comparator = when (mode) {
            SortMode.NAME -> foldersFirst.thenBy { it.name.lowercase() }
            SortMode.DATE -> foldersFirst.thenByDescending { it.lastModified }
            SortMode.SIZE -> foldersFirst.thenByDescending { it.sizeBytes }
        }
        return entries.sortedWith(comparator)
    }

    private fun pathLabel(source: FileSource?, currentPath: String?): String {
        if (source == null) return ""
        if (source is FileSource.Media || currentPath == null) return source.label
        val root = (source as FileSource.Volume).path
        if (currentPath == root) return source.label
        val relative = currentPath.removePrefix(root).trim('/')
        return if (relative.isEmpty()) source.label else "${source.label} / $relative"
    }

    private fun runOp(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = withContext(Dispatchers.IO) {
                runCatching { block() }
            }
            _state.update { it.copy(busy = false) }
            result.exceptionOrNull()?.let { error ->
                if (error is CancellationException) throw error
                _events.tryEmit(FileManagerEvent.Message(R.string.files_op_failed))
            }
            refresh()
        }
    }

    private suspend fun emit(event: FileManagerEvent) {
        _events.emit(event)
    }

    companion object {
        private val PICKER_ACTIONS = setOf(
            Intent.ACTION_GET_CONTENT,
            Intent.ACTION_PICK,
            Intent.ACTION_OPEN_DOCUMENT
        )
    }
}
