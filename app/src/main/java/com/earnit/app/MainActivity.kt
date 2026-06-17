package com.earnit.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import com.earnit.app.ui.EarnItApp
import com.earnit.app.ui.theme.EarnItTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val claimRewardId = mutableStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        claimRewardId.value = intent.getLongExtra("rewardId", 0L)
        setContent {
            EarnItTheme {
                Surface {
                    EarnItApp(startRewardId = claimRewardId.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        claimRewardId.value = intent.getLongExtra("rewardId", 0L)
    }
}
