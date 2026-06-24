@file:OptIn(ExperimentalMaterial3Api::class)

package com.earnit.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.earnit.app.viewmodel.EarnItViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DataScreen(
    viewModel: EarnItViewModel,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var testDataLoaded by remember { mutableStateOf(false) }
    var fullTestDataLoaded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearImportResult() }
    }

    val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            uri?.let {
                viewModel.exportToFile(context, it) { ok ->
                    exportStatus = if (ok) Strings.DATA_EXPORT_SUCCESS else Strings.DATA_EXPORT_FAIL
                }
            }
        }

    val importReplaceLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { viewModel.importFromFile(context, it, replace = true) }
        }

    val importMergeLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { viewModel.importFromFile(context, it, replace = false) }
        }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
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
                Strings.SETTINGS_DATA_TITLE,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Default.Share,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Strings.DATA_EXPORT_TITLE, style = MaterialTheme.typography.titleSmall)
                        Text(
                            Strings.DATA_EXPORT_SUBTITLE,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(
                        onClick = { exportLauncher.launch("earnit_backup_$dateStr.json") },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            "EXPORT",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                exportStatus?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Strings.DATA_IMPORT_TITLE, style = MaterialTheme.typography.titleSmall)
                        Text(
                            Strings.DATA_IMPORT_SUBTITLE,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EarnItPrimaryButton(
                        text = "REPLACE ALL",
                        onClick = { importReplaceLauncher.launch("application/json") },
                        modifier = Modifier.weight(1f),
                    )
                    EarnItOutlinedButton(
                        text = "MERGE",
                        onClick = { importMergeLauncher.launch("application/json") },
                        modifier = Modifier.weight(1f),
                    )
                }
                importResult?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (settings.devModeEnabled) {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Science,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Load test data", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Sample data for general testing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.seedTestData()
                                testDataLoaded = true
                            },
                            enabled = !testDataLoaded,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                if (testDataLoaded) "LOADED" else "LOAD",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }

                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Science,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Load full test data", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "All mascots unlocked, all reward states covered",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.seedFullTestData()
                                fullTestDataLoaded = true
                            },
                            enabled = !fullTestDataLoaded,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                if (fullTestDataLoaded) "LOADED" else "LOAD",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }

                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Developer mode active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.disableDevMode() }) {
                            Text(
                                "DISABLE",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
