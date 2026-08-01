package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.core.database.AppDatabase
import com.example.core.preferences.ThemeMode
import com.example.core.preferences.ThemeRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled by system */ }

    /** Phase 6.5.7 (launch-jank): prompt for the notification permission only after the first
     *  frame has drawn, so the system dialog never competes with the cold-start composition. */
    private var permissionDeferred = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Phase 6.5.7 (launch-jank): do NOT prompt for the notification permission during the
        // first composition — the system GrantPermissionsActivity would contend with the initial
        // draw (cold-start jank). Defer it until after the first frame via onResume+postDelayed.
        val themeRepository = ThemeRepository(applicationContext)

        // Phase 3 backfill engine moved off the cold-start path (Sprint 4, Phase 4.2): the
        // historical behavior_snapshot fill now runs lazily on first Mirror open (see
        // GoalDetailViewModel.refreshMirror), so it no longer contends with first-screen queries.
        setContent {
            // Collect the persisted theme preference
            val themeMode by themeRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val scope = rememberCoroutineScope()

            MyApplicationTheme(themeMode = themeMode) {
                MainScreen(
                    modifier = Modifier.fillMaxSize(),
                    themeMode = themeMode,
                    onThemeChanged = { newMode ->
                        scope.launch {
                            themeRepository.setThemeMode(newMode)
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!permissionDeferred) {
            permissionDeferred = true
            // Defer the permission prompt ~1.2s past first resume so the initial draw + staged
            // entrance are not interrupted by the system GrantPermissionsActivity. One-shot.
            mainHandler.postDelayed({ requestNotificationPermissionIfNeeded() }, 1200L)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}
