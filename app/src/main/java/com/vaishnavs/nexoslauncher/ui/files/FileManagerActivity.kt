package com.vaishnavs.nexoslauncher.ui.files

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.Settings
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vaishnavs.nexoslauncher.R
import com.vaishnavs.nexoslauncher.data.files.FileEntry
import com.vaishnavs.nexoslauncher.data.files.FileTypes
import com.vaishnavs.nexoslauncher.data.files.FileUris
import com.vaishnavs.nexoslauncher.data.files.StorageRepository
import com.vaishnavs.nexoslauncher.databinding.ActivityFileManagerBinding
import kotlinx.coroutines.launch
import java.io.File

class FileManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFileManagerBinding
    private val viewModel: FileManagerViewModel by viewModels()
    private val thumbnailLoader = ThumbnailLoader()
    private var searchOpen = false
    private var volumeCallback: StorageManager.StorageVolumeCallback? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.reloadSources()
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.reloadSources()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.captureIntent(intent)

        val sourceAdapter = FileSourceAdapter { source -> viewModel.selectSource(source) }
        val fileAdapter = FileListAdapter(
            thumbnailLoader = thumbnailLoader,
            onFileClicked = viewModel::onEntryClicked,
            onFileLongClicked = { entry ->
                viewModel.onEntryLongClicked(entry)
                true
            }
        )
        binding.sourceList.layoutManager = LinearLayoutManager(this)
        binding.sourceList.adapter = sourceAdapter
        binding.fileList.layoutManager = LinearLayoutManager(this)
        binding.fileList.adapter = fileAdapter

        binding.upButton.setOnClickListener { viewModel.goUp() }
        binding.sortButton.setOnClickListener { viewModel.cycleSort() }
        binding.searchButton.setOnClickListener { openSearch() }
        binding.searchClose.setOnClickListener { closeSearch() }
        binding.searchInput.doAfterTextChanged { text ->
            viewModel.setQuery(text?.toString().orEmpty())
        }
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            actionId == EditorInfo.IME_ACTION_SEARCH
        }
        binding.grantButton.setOnClickListener { grantAccess() }
        binding.copyButton.setOnClickListener { viewModel.copySelection() }
        binding.cutButton.setOnClickListener { viewModel.cutSelection() }
        binding.pasteButton.setOnClickListener { viewModel.paste() }
        binding.deleteButton.setOnClickListener { confirmDelete() }
        binding.renameButton.setOnClickListener { promptRename() }
        binding.mkdirButton.setOnClickListener { promptMkdir() }
        binding.doneButton.setOnClickListener { viewModel.confirmPick() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    searchOpen -> closeSearch()
                    viewModel.state.value.selectedPaths.isNotEmpty() -> viewModel.clearSelection()
                    viewModel.goUp() -> Unit
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        render(state, sourceAdapter, fileAdapter)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.captureIntent(intent, force = true)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            addDataScheme("file")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(volumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(volumeReceiver, filter)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manager = getSystemService(StorageManager::class.java)
            val callback = object : StorageManager.StorageVolumeCallback() {
                override fun onStateChanged(volume: StorageVolume) {
                    viewModel.reloadSources()
                }
            }
            volumeCallback = callback
            manager.registerStorageVolumeCallback(mainExecutor, callback)
        }
    }

    override fun onStop() {
        unregisterReceiver(volumeReceiver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            volumeCallback?.let { callback ->
                getSystemService(StorageManager::class.java)
                    .unregisterStorageVolumeCallback(callback)
            }
            volumeCallback = null
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadSources()
    }

    private fun render(
        state: FileManagerState,
        sourceAdapter: FileSourceAdapter,
        fileAdapter: FileListAdapter
    ) {
        binding.filesTitle.setText(if (state.picker) R.string.files_select else R.string.files_name)
        binding.pathLabel.text = state.pathLabel
        binding.upButton.isEnabled = state.canGoUp
        binding.upButton.alpha = if (state.canGoUp) 1f else 0.4f
        sourceAdapter.submitList(
            state.sources.map { source ->
                FileSourceRow(source, selected = source.id == state.selectedSourceId)
            }
        )
        fileAdapter.submitList(
            state.entries.map { entry ->
                FileRow(entry, selected = entry.path in state.selectedPaths)
            }
        )

        val showPermission = state.permissionNeeded
        val showEmpty = !showPermission && !state.busy && state.entries.isEmpty()
        binding.permissionPanel.isVisible = showPermission
        binding.emptyView.isVisible = showEmpty
        binding.emptyView.setText(
            if (state.query.isNotBlank()) R.string.files_empty_search else R.string.files_empty
        )
        binding.fileList.isVisible = !showPermission && state.entries.isNotEmpty()
        binding.progress.isVisible = state.busy

        val hasSelection = state.selectedPaths.isNotEmpty()
        val mutating = !state.picker && !state.permissionNeeded
        binding.copyButton.isVisible = mutating
        binding.cutButton.isVisible = mutating
        binding.pasteButton.isVisible = mutating
        binding.deleteButton.isVisible = mutating
        binding.renameButton.isVisible = mutating
        binding.mkdirButton.isVisible = mutating
        binding.copyButton.isEnabled = hasSelection
        binding.cutButton.isEnabled = hasSelection
        binding.deleteButton.isEnabled = hasSelection
        binding.renameButton.isEnabled = state.selectedPaths.size == 1
        binding.pasteButton.isEnabled = state.canPaste
        binding.mkdirButton.isEnabled = !state.isVirtual && state.currentPath != null
        binding.doneButton.isVisible = state.picker
        binding.doneButton.isEnabled = state.entries.any { it.path in state.selectedPaths && !it.isDirectory }
        setActionAlpha(binding.copyButton, binding.copyButton.isEnabled)
        setActionAlpha(binding.cutButton, binding.cutButton.isEnabled)
        setActionAlpha(binding.pasteButton, binding.pasteButton.isEnabled)
        setActionAlpha(binding.deleteButton, binding.deleteButton.isEnabled)
        setActionAlpha(binding.renameButton, binding.renameButton.isEnabled)
        setActionAlpha(binding.mkdirButton, binding.mkdirButton.isEnabled)
    }

    private fun setActionAlpha(view: android.view.View, enabled: Boolean) {
        view.alpha = if (enabled) 1f else 0.35f
    }

    private fun handleEvent(event: FileManagerEvent) {
        when (event) {
            is FileManagerEvent.Message -> {
                val text = if (event.args.isEmpty()) {
                    getString(event.resId)
                } else {
                    getString(event.resId, *event.args.toTypedArray())
                }
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
            }
            is FileManagerEvent.OpenFile -> openFile(event.entry)
            is FileManagerEvent.PickFiles -> returnPicked(event.paths)
        }
    }

    private fun openFile(entry: FileEntry) {
        val uri = runCatching { FileUris.uriFor(this, entry.file) }.getOrNull()
        if (uri == null) {
            Toast.makeText(this, R.string.files_open_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, entry.mimeType ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
            .onFailure {
                Toast.makeText(this, R.string.files_open_failed, Toast.LENGTH_SHORT).show()
            }
    }

    private fun returnPicked(paths: List<String>) {
        val files = paths.map { File(it) }.filter { it.isFile }
        val uris = files.mapNotNull { file ->
            runCatching { FileUris.uriFor(this, file) }.getOrNull()
        }
        if (uris.isEmpty()) {
            Toast.makeText(this, R.string.files_op_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val result = Intent().addFlags(flags)
        val mime = FileTypes.mimeOf(files.first()) ?: "*/*"
        if (uris.size == 1) {
            result.setDataAndType(uris.first(), mime)
        } else {
            result.type = "*/*"
            val clip = ClipData.newUri(contentResolver, getString(R.string.files_name), uris.first())
            uris.drop(1).forEach { uri -> clip.addItem(ClipData.Item(uri)) }
            result.clipData = clip
        }
        callingPackage?.let { pkg ->
            uris.forEach { uri -> grantUriPermission(pkg, uri, flags) }
        }
        setResult(RESULT_OK, result)
        finish()
    }

    private fun grantAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !viewModel.hasFullAccess()) {
            val appIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData(Uri.parse("package:$packageName"))
            val launched = runCatching { startActivity(appIntent) }.isSuccess
            if (!launched) {
                runCatching {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        }
        val runtime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            StorageRepository.MEDIA_PERMISSIONS
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            StorageRepository.LEGACY_STORAGE_PERMISSIONS
        } else {
            emptyArray()
        }
        if (runtime.isNotEmpty()) {
            permissionLauncher.launch(runtime)
        }
    }

    private fun confirmDelete() {
        val selected = viewModel.selectedEntries()
        if (selected.isEmpty()) return
        val message = if (selected.size == 1) {
            getString(R.string.files_delete_one, selected.first().name)
        } else {
            getString(R.string.files_delete_many, selected.size)
        }
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.files_delete) { _, _ -> viewModel.deleteSelection() }
            .show()
    }

    private fun promptRename() {
        val current = viewModel.selectedEntries().singleOrNull()?.name ?: return
        promptText(R.string.files_rename, R.string.files_rename_hint, current) { name ->
            viewModel.renameSelection(name)
        }
    }

    private fun promptMkdir() {
        promptText(R.string.files_new_folder, R.string.files_folder_hint, "") { name ->
            viewModel.createFolder(name)
        }
    }

    private fun promptText(
        title: Int,
        hint: Int,
        initial: String,
        onConfirm: (String) -> Unit
    ) {
        val input = EditText(this).apply {
            setText(initial)
            setHint(hint)
            setTextColor(ContextCompat.getColor(this@FileManagerActivity, R.color.white))
            setHintTextColor(ContextCompat.getColor(this@FileManagerActivity, R.color.grey))
            setSelection(initial.length)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val pad = (16 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ -> onConfirm(input.text.toString()) }
            .show()
    }

    private fun openSearch() {
        searchOpen = true
        binding.searchBar.isVisible = true
        binding.searchButton.isVisible = false
        binding.searchInput.requestFocus()
    }

    private fun closeSearch() {
        searchOpen = false
        binding.searchBar.isVisible = false
        binding.searchButton.isVisible = true
        binding.searchInput.setText("")
        viewModel.setQuery("")
    }
}
