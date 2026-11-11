package com.driveup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.driveup.data.repository.TimetableRepository
import com.driveup.ui.import_.ImportScreen
import com.driveup.ui.main.MainScreen
import com.driveup.ui.theme.DriveUpTheme
import com.driveup.ui.theme.ThemeMode
import com.driveup.utils.NotificationHelper
import com.driveup.utils.ThemePreference

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(ThemePreference.get(context)) }

            DriveUpTheme(themeMode = themeMode) {
                val repository = remember { TimetableRepository(context) }
                AppRoot(
                    repository    = repository,
                    themeMode     = themeMode,
                    onThemeChange = { mode ->
                        themeMode = mode
                        ThemePreference.set(context, mode)
                    }
                )
            }
        }
    }
}

@Composable
fun AppRoot(
    repository: TimetableRepository,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val hasSemester by repository.hasSemester().collectAsState(initial = null)
    var forceImport by remember { mutableStateOf(false) }

    when {
        hasSemester == null -> {}
        !hasSemester!! || forceImport -> ImportScreen(
            repository = repository,
            onImportComplete = { forceImport = false }
        )
        else -> MainScreen(
            repository        = repository,
            themeMode         = themeMode,
            onThemeChange     = onThemeChange,
            onRequestReimport = { forceImport = true }
        )
    }
}
