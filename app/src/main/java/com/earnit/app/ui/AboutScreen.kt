@file:OptIn(ExperimentalMaterial3Api::class)

package com.earnit.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.earnit.app.BuildConfig
import com.earnit.app.FeatureFlags
import com.earnit.app.viewmodel.EarnItViewModel
import com.earnit.app.viewmodel.TipViewModel
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    navController: NavHostController,
    viewModel: EarnItViewModel = hiltViewModel(),
    tipViewModel: TipViewModel = hiltViewModel(),
) {
    val tipState by tipViewModel.tipState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var devTapCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        tipViewModel.purchaseEvent.collect { event ->
            when (event) {
                is TipViewModel.PurchaseEvent.Success -> snackbarHostState.showSnackbar(Strings.TIP_SUCCESS)
                is TipViewModel.PurchaseEvent.Cancelled -> Unit
                is TipViewModel.PurchaseEvent.Error -> snackbarHostState.showSnackbar(Strings.TIP_ERROR)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Strings.BACK_DESC,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    Strings.APP_NAME,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column {
                    Text(
                        "Version: ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier.clickable {
                                if (settings.devModeEnabled) return@clickable
                                devTapCount++
                                if (devTapCount >= 7) {
                                    scope.launch {
                                        viewModel.enableDevMode()
                                        snackbarHostState.showSnackbar("Developer options enabled")
                                    }
                                }
                            },
                    )
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        Strings.ABOUT_THE_IDEA_TITLE,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        Strings.ABOUT_THE_IDEA_BODY,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                HorizontalDivider()

                AboutActionRow(
                    emoji = "⭐",
                    label = Strings.ABOUT_RATE_LABEL,
                    subtitle = Strings.ABOUT_RATE_SUBTITLE,
                    onClick = {
                        val packageName = context.packageName
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                                    .addFlags(
                                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
                                    ),
                            )
                        } catch (e: ActivityNotFoundException) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                                ),
                            )
                        }
                    },
                )

                AboutActionRow(
                    emoji = "✉️",
                    label = Strings.ABOUT_CONTACT_LABEL,
                    subtitle = Strings.ABOUT_CONTACT_SUBTITLE,
                    onClick = {
                        val intent =
                            Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${Strings.ABOUT_CONTACT_EMAIL}")
                                putExtra(Intent.EXTRA_SUBJECT, "Earn It feedback")
                            }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                )

                if (FeatureFlags.TIP_JAR_ENABLED) {
                    HorizontalDivider()

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            Strings.ABOUT_TIP_TITLE,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            Strings.ABOUT_TIP_COPY,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        when (val state = tipState) {
                            is TipViewModel.TipState.Loading -> {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                                }
                            }
                            is TipViewModel.TipState.Ready -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    state.options.forEach { option ->
                                        EarnItPrimaryButton(
                                            text = "${option.title} · ${option.formattedPrice}",
                                            onClick = {
                                                (context as? Activity)?.let { activity ->
                                                    tipViewModel.purchase(activity, option.productId)
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                            is TipViewModel.TipState.Error -> {
                                Text(
                                    Strings.TIP_LOAD_ERROR,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AboutActionRow(
    emoji: String,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
