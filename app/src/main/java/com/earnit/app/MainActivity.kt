package com.earnit.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import com.earnit.app.ui.EarnItApp
import com.earnit.app.ui.theme.EarnItTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val claimRewardId = mutableStateOf(0L)
    private val autoOpenAddTask = mutableStateOf(false)

    // Bumped on every onCreate/onNewIntent so EarnItApp's nav effect re-fires even when
    // the extras repeat (e.g. tapping the widget's ADD TASK button twice in a row for the
    // same reward while the app is already showing it) — a plain state-equality key would
    // silently no-op on the second identical intent.
    private val navRequestToken = mutableStateOf(0)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        claimRewardId.value = intent.getLongExtra("rewardId", 0L)
        autoOpenAddTask.value = intent.getBooleanExtra("autoOpenAddTask", false)
        navRequestToken.value++
        setContent {
            EarnItTheme {
                Surface {
                    EarnItApp(
                        startRewardId = claimRewardId.value,
                        autoOpenAddTask = autoOpenAddTask.value,
                        navRequestToken = navRequestToken.value,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        claimRewardId.value = intent.getLongExtra("rewardId", 0L)
        autoOpenAddTask.value = intent.getBooleanExtra("autoOpenAddTask", false)
        navRequestToken.value++
    }
}
