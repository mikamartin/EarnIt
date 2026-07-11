@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.earnit.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.earnit.app.data.taskTemplates
import com.earnit.app.viewmodel.EarnItViewModel

@Composable
fun TaskLibraryScreen(
    viewModel: EarnItViewModel,
    navController: NavHostController,
) {
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }
    val checkedMap = remember { mutableStateMapOf<String, Boolean>() }
    var skippedDialog by remember { mutableStateOf<List<String>?>(null) }

    if (skippedDialog != null) {
        val names = skippedDialog!!
        AlertDialog(
            onDismissRequest = {
                skippedDialog = null
                navController.popBackStack()
            },
            title = {
                Text(
                    Strings.librarySkippedTitle(names.size),
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            text = { Text(Strings.librarySkippedBody(names)) },
            confirmButton = {
                EarnItPrimaryButton(
                    text = "OK",
                    onClick = {
                        skippedDialog = null
                        navController.popBackStack()
                    },
                )
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
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
                Strings.LIBRARY_TITLE,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(taskTemplates) { tmpl ->
                val expanded = expandedMap[tmpl.name] ?: false
                val selectedTasks = tmpl.tasks.filter { t -> checkedMap["${tmpl.name}_${t.name}"] != false }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        // ── Template header ───────────────────────────────────
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedMap[tmpl.name] = !expanded }
                                    .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(tmpl.icon, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                tmpl.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                Strings.libraryTaskCount(tmpl.tasks.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // ── Expanded task list ────────────────────────────────
                        AnimatedVisibility(visible = expanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Spacer(Modifier.height(4.dp))
                                tmpl.tasks.forEach { t ->
                                    val key = "${tmpl.name}_${t.name}"
                                    val checked = checkedMap.getOrPut(key) { true }

                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Min)
                                                .clickable { checkedMap[key] = !checked },
                                    ) {
                                        // Accent bar — mirrors the left bar on task cards
                                        Box(
                                            modifier =
                                                Modifier
                                                    .width(4.dp)
                                                    .fillMaxHeight()
                                                    .background(
                                                        if (checked) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                alpha = 0.2f,
                                                            )
                                                        },
                                                    ),
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier =
                                                Modifier
                                                    .weight(1f)
                                                    .padding(top = 4.dp, bottom = 4.dp, end = 8.dp),
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = { checkedMap[key] = it },
                                                colors =
                                                    CheckboxDefaults.colors(
                                                        checkedColor = MaterialTheme.colorScheme.primary,
                                                        uncheckedColor =
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                alpha = 0.5f,
                                                            ),
                                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                                    ),
                                            )
                                            Text(t.icon, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                t.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color =
                                                    if (checked) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                    },
                                                modifier = Modifier.weight(1f),
                                            )
                                            Text(
                                                "+${t.points}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color =
                                                    if (checked) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                    },
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    Strings.libraryGroupHint(tmpl.icon, tmpl.name),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(8.dp))
                                EarnItPrimaryButton(
                                    text = Strings.libraryAddButton(selectedTasks.size),
                                    enabled = selectedTasks.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        viewModel.importTemplate(
                                            tmpl.copy(tasks = selectedTasks),
                                            cleanSlate = false,
                                        ) { skipped ->
                                            if (skipped.isEmpty()) {
                                                navController.popBackStack()
                                            } else {
                                                skippedDialog = skipped
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
