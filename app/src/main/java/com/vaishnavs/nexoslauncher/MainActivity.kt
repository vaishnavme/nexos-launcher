package com.vaishnavs.nexoslauncher

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaishnavs.nexoslauncher.data.apps.AppRepository
import com.vaishnavs.nexoslauncher.data.prefs.DockPrefs
import com.vaishnavs.nexoslauncher.databinding.ActivityMainBinding
import com.vaishnavs.nexoslauncher.system.AppIntents
import com.vaishnavs.nexoslauncher.system.OemAppResolver
import com.vaishnavs.nexoslauncher.ui.apps.AppsFragment
import com.vaishnavs.nexoslauncher.ui.dock.DockAdapter
import com.vaishnavs.nexoslauncher.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dockPrefs: DockPrefs
    private lateinit var appRepository: AppRepository
    private lateinit var dockAdapter: DockAdapter

    private val homeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Home role accepted or dismissed. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appRepository = AppRepository(packageManager, packageName)
        val resolver = OemAppResolver(packageManager)
        dockPrefs = DockPrefs(this).also { prefs ->
            prefs.ensureDefaults(resolver)
        }
        dockAdapter = DockAdapter(
            iconProvider = { component ->
                runCatching { appRepository.loadIcon(component) }.getOrNull()
            },
            onAppClicked = ::launchComponent
        )
        binding.dockList.layoutManager = LinearLayoutManager(this)
        binding.dockList.adapter = dockAdapter
        binding.dockList.itemAnimator = null
        binding.dockHomeApps.setOnClickListener { toggleHomeApps() }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val apps = supportFragmentManager.findFragmentById(R.id.content_container)
                    if (apps is AppsFragment && apps.closeSearchOrConsumeBack()) {
                        return
                    }
                    if (apps is AppsFragment) {
                        showHome()
                    }
                }
            }
        )

        refreshDock()
        updateHomeAppsButton()
        offerAsDefaultHome()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showHome()
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        refreshDock()
        hideSystemBars()
    }

    private fun offerAsDefaultHome() {
        val roleManager = getSystemService(RoleManager::class.java) ?: return
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) return
        if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) return
        runCatching {
            homeRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun refreshDock() {
        dockAdapter.submit(dockPrefs.loadSlots())
    }

    private fun launchComponent(component: ComponentName) {
        try {
            AppIntents.start(this, component)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleHomeApps() {
        if (supportFragmentManager.findFragmentById(R.id.content_container) is AppsFragment) {
            showHome()
        } else {
            showApps()
        }
    }

    private fun showApps() {
        if (supportFragmentManager.findFragmentById(R.id.content_container) is AppsFragment) {
            return
        }
        supportFragmentManager.commit {
            replace(R.id.content_container, AppsFragment())
        }
        updateHomeAppsButton(appsOpen = true)
    }

    private fun showHome() {
        supportFragmentManager.commit {
            replace(R.id.content_container, HomeFragment())
        }
        updateHomeAppsButton(appsOpen = false)
    }

    private fun updateHomeAppsButton(appsOpen: Boolean? = null) {
        val open = appsOpen
            ?: (supportFragmentManager.findFragmentById(R.id.content_container) is AppsFragment)
        if (open) {
            binding.dockHomeApps.setImageResource(R.drawable.ic_home)
            binding.dockHomeApps.contentDescription = getString(R.string.dock_home)
        } else {
            binding.dockHomeApps.setImageResource(R.drawable.ic_apps)
            binding.dockHomeApps.contentDescription = getString(R.string.dock_apps)
        }
    }
}
