package com.vaishnavs.nexoslauncher.ui.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.vaishnavs.nexoslauncher.data.apps.AppInfo
import com.vaishnavs.nexoslauncher.databinding.FragmentAppsBinding
import com.vaishnavs.nexoslauncher.system.AppIntents
import kotlinx.coroutines.launch

class AppsFragment : Fragment() {
    private var binding: FragmentAppsBinding? = null
    private val viewModel: AppsViewModel by viewModels()
    private var searchOpen = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentAppsBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = AppGridAdapter(
            iconProvider = viewModel::iconFor,
            onAppClicked = ::launchApp
        )
        val appsBinding = binding ?: return
        appsBinding.appGrid.layoutManager = GridLayoutManager(requireContext(), GRID_COLUMNS)
        appsBinding.appGrid.adapter = adapter
        appsBinding.appGrid.setHasFixedSize(true)

        appsBinding.searchButton.setOnClickListener { openSearch() }
        appsBinding.searchClose.setOnClickListener { closeSearch() }
        appsBinding.searchInput.doAfterTextChanged { text ->
            updateSearchResults(text?.toString().orEmpty())
        }
        appsBinding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val first = viewModel.topMatches(appsBinding.searchInput.text.toString()).firstOrNull()
                if (first != null) launchApp(first)
                true
            } else {
                false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.apps.collect { apps ->
                    adapter.submitList(apps)
                    appsBinding.emptyView.isVisible = apps.isEmpty()
                    appsBinding.appGrid.isVisible = apps.isNotEmpty()
                    if (searchOpen) {
                        updateSearchResults(appsBinding.searchInput.text.toString())
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        restoreSoftInput()
        binding = null
        super.onDestroyView()
    }

    fun closeSearchOrConsumeBack(): Boolean {
        if (!searchOpen) return false
        closeSearch()
        return true
    }

    private fun openSearch() {
        val appsBinding = binding ?: return
        searchOpen = true
        appsBinding.appsTitle.isVisible = false
        appsBinding.searchButton.isVisible = false
        appsBinding.searchBar.isVisible = true
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        appsBinding.searchInput.requestFocus()
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm.showSoftInput(appsBinding.searchInput, 0)
    }

    private fun closeSearch() {
        val appsBinding = binding ?: return
        searchOpen = false
        appsBinding.searchInput.text = null
        appsBinding.searchBar.isVisible = false
        appsBinding.searchResults.isVisible = false
        appsBinding.appsTitle.isVisible = true
        appsBinding.searchButton.isVisible = true
        hideKeyboard()
        restoreSoftInput()
    }

    private fun updateSearchResults(query: String) {
        val appsBinding = binding ?: return
        val matches = viewModel.topMatches(query)
        val rows = listOf(
            appsBinding.searchResult1,
            appsBinding.searchResult2,
            appsBinding.searchResult3
        )
        appsBinding.searchResults.isVisible = matches.isNotEmpty()
        rows.forEachIndexed { index, row ->
            val app = matches.getOrNull(index)
            row.root.isVisible = app != null
            if (app != null) {
                row.resultLabel.text = app.label
                row.resultIcon.setImageDrawable(viewModel.iconFor(app))
                row.root.setOnClickListener { launchApp(app) }
            } else {
                row.root.setOnClickListener(null)
            }
        }
    }

    private fun launchApp(app: AppInfo) {
        AppIntents.start(requireContext(), app.componentName)
    }

    private fun hideKeyboard() {
        val appsBinding = binding ?: return
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(appsBinding.searchInput.windowToken, 0)
        appsBinding.searchInput.clearFocus()
    }

    private fun restoreSoftInput() {
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }

    companion object {
        private const val GRID_COLUMNS = 4
    }
}
