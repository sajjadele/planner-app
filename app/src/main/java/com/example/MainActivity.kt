package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalRepository
import com.example.core.goal.RoomGoalRepository
import com.example.core.snapshot.RoomSnapshotRepository
import com.example.core.snapshot.SnapshotAggregator
import com.example.core.preferences.ThemeMode
import com.example.core.preferences.ThemeRepository
import com.example.plugins.planner.data.RoomInsightRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled by system */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val themeRepository = ThemeRepository(applicationContext)

        // Phase 3: App-Launch Backfill Engine — fill any missing daily snapshots once per launch.
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            SnapshotAggregator(
                RoomInsightRepository(db.insightDao()),
                RoomSnapshotRepository(db.snapshotDao()),
                RoomGoalRepository(db.goalDao(), db.goalEventDao())
            ).backfillIfNeeded()
        }

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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}
