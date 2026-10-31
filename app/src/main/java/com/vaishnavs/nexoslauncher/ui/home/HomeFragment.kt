package com.vaishnavs.nexoslauncher.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.vaishnavs.nexoslauncher.R
import com.vaishnavs.nexoslauncher.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var binding: FragmentHomeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentHomeBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val metrics = resources.displayMetrics
        binding?.displayMetrics?.text = getString(
            R.string.home_display_metrics,
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi
        )
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
