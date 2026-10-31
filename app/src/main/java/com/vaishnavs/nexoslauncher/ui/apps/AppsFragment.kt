package com.vaishnavs.nexoslauncher.ui.apps

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.vaishnavs.nexoslauncher.data.apps.AppInfo
import com.vaishnavs.nexoslauncher.databinding.FragmentAppsBinding
import kotlinx.coroutines.launch
import kotlin.math.max

class AppsFragment : Fragment() {
    private var binding: FragmentAppsBinding? = null
    private val viewModel: AppsViewModel by viewModels()

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
        appsBinding.appGrid.layoutManager = GridLayoutManager(requireContext(), spanCount())
        appsBinding.appGrid.adapter = adapter
        appsBinding.appGrid.setHasFixedSize(true)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.apps.collect { apps ->
                    adapter.submitList(apps)
                    appsBinding.emptyView.isVisible = apps.isEmpty()
                    appsBinding.appGrid.isVisible = apps.isNotEmpty()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun spanCount(): Int {
        val widthPx = resources.displayMetrics.widthPixels
        val dockWidth = resources.getDimensionPixelSize(
            com.vaishnavs.nexoslauncher.R.dimen.dock_width
        )
        val cellWidth = resources.getDimensionPixelSize(
            com.vaishnavs.nexoslauncher.R.dimen.app_cell_width
        )
        return max(3, (widthPx - dockWidth) / cellWidth)
    }

    private fun launchApp(app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.componentName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
