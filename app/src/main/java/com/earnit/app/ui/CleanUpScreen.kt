@file:OptIn(ExperimentalMaterial3Api::class)

package com.earnit.app.ui

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.earnit.app.viewmodel.EarnItViewModel
import kotlinx.coroutines.launch

@Composable
fun CleanUpScreen(
    viewModel: EarnItViewModel,
    navController: NavHostController,
) {
    var clearLogsDialog by remember { mutableStateOf(false) }
    var clearTasksDialog by remember { mutableStateOf(false) }
    var clearRewardsDialog by remember { mutableStateOf(false) }
    var clearAllDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (clearLogsDialog) {
        AlertDialog(
            onDismissRequest = { clearLogsDialog = false },
            title = { Text(Strings.CLEANUP_DIALOG_LOGS_TITLE, color = MaterialTheme.colorScheme.primary) },
            text = { Text(Strings.CLEANUP_DIALOG_LOGS_BODY) },
            confirmButton = {
                Button(
                    onClick = {
                        clearLogsDialog = false
                        viewModel.clearAllLogs {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    Strings.CLEANUP_SNACKBAR_LOGS,
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                        ),
                ) {
                    Text(
                        "CLEAR",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clearLogsDialog = false
                }) {
                    Text(
                        "CANCEL",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            },
        )
    }
    if (clearTasksDialog) {
        AlertDialog(
            onDismissRequest = { clearTasksDialog = false },
            title = { Text(Strings.CLEANUP_DIALOG_TASKS_TITLE, color = MaterialTheme.colorScheme.primary) },
            text = { Text(Strings.CLEANUP_DIALOG_TASKS_BODY) },
            confirmButton = {
                Button(
                    onClick = {
                        clearTasksDialog = false
                        viewModel.clearAllTasks {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    Strings.CLEANUP_SNACKBAR_TASKS,
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                        ),
                ) {
                    Text(
                        "CLEAR",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clearTasksDialog = false
                }) {
                    Text(
                        "CANCEL",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            },
        )
    }
    if (clearRewardsDialog) {
        AlertDialog(
            onDismissRequest = { clearRewardsDialog = false },
            title = { Text(Strings.CLEANUP_DIALOG_REWARDS_TITLE, color = MaterialTheme.colorScheme.primary) },
            text = { Text(Strings.CLEANUP_DIALOG_REWARDS_BODY) },
            confirmButton = {
                Button(
                    onClick = {
                        clearRewardsDialog = false
                        viewModel.clearAllRewards {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    Strings.CLEANUP_SNACKBAR_REWARDS,
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                        ),
                ) {
                    Text(
                        "CLEAR",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clearRewardsDialog = false
                }) {
                    Text(
                        "CANCEL",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            },
        )
    }
    if (clearAllDialog) {
        AlertDialog(
            onDismissRequest = { clearAllDialog = false },
            title = { Text(Strings.CLEANUP_DIALOG_ALL_TITLE, color = MaterialTheme.colorScheme.error) },
            text = { Text(Strings.CLEANUP_DIALOG_ALL_BODY) },
            confirmButton = {
                Button(
                    onClick = {
                        clearAllDialog = false
                        viewModel.clearAll {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    Strings.CLEANUP_SNACKBAR_ALL,
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                        ),
                ) {
                    Text(
                        "WIPE EVERYTHING",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clearAllDialog = false
                }) {
                    Text(
                        "CANCEL",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            },
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding)) {
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
                    Strings.CLEANUP_SCREEN_TITLE,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp),
                        )
                        Text(
                            Strings.CLEANUP_DISCLAIMER,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                SettingsCard(borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            Strings.CLEANUP_CARD_LOGS,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        DangerButton(Strings.CLEANUP_BTN_LOGS) { clearLogsDialog = true }
                    }
                }

                SettingsCard(borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            Strings.CLEANUP_CARD_TASKS,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        DangerButton(Strings.CLEANUP_BTN_TASKS) { clearTasksDialog = true }
                    }
                }

                SettingsCard(borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            Strings.CLEANUP_CARD_REWARDS,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        DangerButton(Strings.CLEANUP_BTN_REWARDS) { clearRewardsDialog = true }
                    }
                }

                SettingsCard(borderColor = MaterialTheme.colorScheme.error) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            Strings.CLEANUP_CARD_ALL,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        DangerButton(Strings.CLEANUP_BTN_ALL) { clearAllDialog = true }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    } // Scaffold
}
