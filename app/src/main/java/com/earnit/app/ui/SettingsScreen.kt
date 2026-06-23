@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.earnit.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.earnit.app.BuildConfig
import com.earnit.app.data.AppColorScheme
import com.earnit.app.data.AppSettings
import com.earnit.app.data.MascotId
import com.earnit.app.data.Mascots
import com.earnit.app.viewmodel.EarnItViewModel
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun SettingsScreen(
    viewModel: EarnItViewModel,
    settings: AppSettings,
    navController: NavHostController,
) {
    var optimalText by remember(settings.optimalRewardCount) { mutableStateOf(settings.optimalRewardCount.toString()) }
    var maxText by remember(settings.maxRewardCount) { mutableStateOf(settings.maxRewardCount.toString()) }
    var showRewardsInfo by remember { mutableStateOf(false) }
    var showTasksInfo by remember { mutableStateOf(false) }
    var showMascotPicker by remember { mutableStateOf(false) }
    var highlightedMascot by remember { mutableStateOf<MascotId?>(null) }

    LaunchedEffect(Unit) {
        viewModel.openMascotPicker
            .filterNotNull()
            .collect { mascotId ->
                highlightedMascot = mascotId
                showMascotPicker = true
                viewModel.consumeMascotPickerId()
            }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── About ─────────────────────────────────────────────────────────────
        SettingsSectionHeader(Strings.SETTINGS_SECTION_ABOUT)
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.About.route) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            Strings.APP_NAME,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Version: ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        Strings.ABOUT_TEASER,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // ── Appearance ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                Strings.SETTINGS_SECTION_APPEARANCE,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
        }
        SettingsCard {
            val focusManager = LocalFocusManager.current
            var nicknameText by remember(settings.nickname) { mutableStateOf(settings.nickname) }
            var showNameInfo by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    Text(
                        Strings.SETTINGS_NAME_LABEL,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    InfoIconButton(expanded = showNameInfo, onClick = { showNameInfo = !showNameInfo })
                }
                if (showNameInfo) {
                    Text(
                        Strings.NAME_INFO,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                    )
                }
                OutlinedTextField(
                    value = if (settings.useRandomNickname) viewModel.sessionNickname else nicknameText,
                    onValueChange = { newValue ->
                        nicknameText = newValue
                        if (settings.useRandomNickname) viewModel.updateUseRandomNickname(false)
                    },
                    placeholder = { Text(Strings.SETTINGS_NAME_PLACEHOLDER) },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                if (!it.isFocused &&
                                    !settings.useRandomNickname
                                ) {
                                    viewModel.updateNickname(nicknameText.trim())
                                }
                            },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(onDone = {
                            if (!settings.useRandomNickname) {
                                viewModel.updateNickname(nicknameText.trim())
                                focusManager.clearFocus()
                            }
                        }),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.refreshNickname() }) {
                            Icon(
                                if (settings.useRandomNickname) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                contentDescription = "Random name",
                                tint =
                                    if (settings.useRandomNickname) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    },
                )
            }
        }
        SettingsCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeChip(
                    Strings.THEME_WARM_GOLD,
                    AppColorScheme.WARM_GOLD,
                    Color(0xFFE8A000),
                    Color(0xFF2A9D8F),
                    settings.colorScheme,
                    Modifier.weight(1f),
                ) {
                    viewModel.updateColorScheme(it)
                }
                ThemeChip(
                    Strings.THEME_OCEAN_BLUE,
                    AppColorScheme.OCEAN_BLUE,
                    Color(0xFF1976D2),
                    Color(0xFF0097A7),
                    settings.colorScheme,
                    Modifier.weight(1f),
                ) {
                    viewModel.updateColorScheme(it)
                }
                ThemeChip(
                    Strings.THEME_FOREST,
                    AppColorScheme.FOREST,
                    Color(0xFF2E7D32),
                    Color(0xFF795548),
                    settings.colorScheme,
                    Modifier.weight(1f),
                ) {
                    viewModel.updateColorScheme(it)
                }
            }
        }
        SettingsCard {
            val currentMascotName =
                settings.selectedMascotId
                    ?.let { id -> Mascots.all.find { it.id == id }?.displayName }
                    ?: Strings.MASCOT_NONE_LABEL
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMascotPicker = true
                            highlightedMascot = null
                        },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    Strings.MASCOT_SECTION_TITLE,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    currentMascotName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (showMascotPicker) {
                MascotPickerDialog(
                    settings = settings,
                    highlightedMascot = highlightedMascot,
                    onSelect = {
                        viewModel.updateSelectedMascot(it)
                        showMascotPicker = false
                        highlightedMascot =
                            null
                    },
                    onDismiss = {
                        showMascotPicker = false
                        highlightedMascot = null
                    },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    Strings.SETTINGS_QUOTE_TOGGLE,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = settings.showQuote,
                    onCheckedChange = { viewModel.updateShowQuote(it) },
                )
            }
        }

        // ── Rewards ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                Strings.SETTINGS_SECTION_REWARDS,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            InfoIconButton(expanded = showRewardsInfo, onClick = { showRewardsInfo = !showRewardsInfo })
        }
        SettingsCard {
            if (showRewardsInfo) {
                Text(
                    Strings.REWARDS_INFO,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = optimalText,
                    onValueChange = { v ->
                        if (v.length <= 2) {
                            optimalText = v
                            v.toIntOrNull()?.let { if (it > 0) viewModel.updateOptimalRewardCount(it) }
                        }
                    },
                    label = { Text(Strings.SETTINGS_OPTIMAL_LABEL, style = MaterialTheme.typography.labelSmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = { v ->
                        if (v.length <= 2) {
                            maxText = v
                            v.toIntOrNull()?.let { if (it > 0) viewModel.updateMaxRewardCount(it) }
                        }
                    },
                    label = { Text(Strings.SETTINGS_MAX_LABEL, style = MaterialTheme.typography.labelSmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Tasks ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                Strings.SETTINGS_SECTION_TASKS,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            InfoIconButton(expanded = showTasksInfo, onClick = { showTasksInfo = !showTasksInfo })
        }
        SettingsCard {
            if (showTasksInfo) {
                Text(
                    Strings.TASKS_NOTES_INFO,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    Strings.SETTINGS_NOTES_TOGGLE,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = settings.notesMandatory,
                    onCheckedChange = { viewModel.updateNotesMandatory(it) },
                    modifier = Modifier.semantics { contentDescription = Strings.SETTINGS_NOTES_DESC },
                )
            }
        }

        // ── Data ─────────────────────────────────────────────────────────────
        SettingsSectionHeader(Strings.SETTINGS_SECTION_DATA)
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.SettingsData.route) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.Save,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(Strings.SETTINGS_DATA_TITLE, style = MaterialTheme.typography.titleSmall)
                    Text(
                        Strings.SETTINGS_DATA_SUBTITLE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // ── Clean Up ─────────────────────────────────────────────────────────
        SettingsSectionHeader(Strings.SETTINGS_SECTION_CLEANUP)
        SettingsCard(borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)) {
            Row(
                modifier =
                    Modifier.fillMaxWidth().clickable {
                        navController.navigate(
                            Screen.SettingsCleanUp.route,
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    Strings.SETTINGS_CLEANUP_ROW,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsSectionHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
internal fun SettingsCard(
    borderColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ThemeChip(
    label: String,
    scheme: AppColorScheme,
    primary: Color,
    secondary: Color,
    current: AppColorScheme,
    modifier: Modifier = Modifier,
    onSelect: (AppColorScheme) -> Unit,
) {
    val selected = scheme == current
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                ).clickable { onSelect(scheme) }
                .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(primary))
            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(secondary))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MascotPickerDialog(
    settings: AppSettings,
    highlightedMascot: MascotId?,
    onSelect: (MascotId?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 24.dp),
        title = null,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        Strings.MASCOT_PICKER_TITLE,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Switch(
                        checked = settings.selectedMascotId != null,
                        onCheckedChange = { show ->
                            if (show) onSelect(MascotId.PUGSLY) else onSelect(null)
                        },
                    )
                }
                HorizontalDivider()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(Mascots.all) { index, mascot ->
                        val unlocked = mascot.id in settings.unlockedMascotIds
                        val selected = mascot.id == settings.selectedMascotId
                        val prevUnlocked = index == 0 || Mascots.all[index - 1].id in settings.unlockedMascotIds
                        val drawable = if (unlocked) mascot.drawable else null
                        val primary = MaterialTheme.colorScheme.primary
                        Column(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) {
                                            primary.copy(
                                                alpha = 0.12f,
                                            )
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                    ).then(if (unlocked) Modifier.clickable { onSelect(mascot.id) } else Modifier)
                                    .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (drawable != null) {
                                    Image(
                                        painter = painterResource(drawable),
                                        contentDescription = mascot.displayName,
                                        modifier = Modifier.size(56.dp),
                                    )
                                } else {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            if (unlocked) Icons.Default.AutoAwesome else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                                if (selected) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(primary)
                                                .align(Alignment.TopEnd),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }
                                if (mascot.id == highlightedMascot) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondary)
                                                .align(Alignment.TopStart),
                                    )
                                }
                            }
                            if (unlocked) {
                                Text(
                                    text = mascot.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            } else if (prevUnlocked) {
                                Text(
                                    text = mascot.unlockHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
internal fun DangerButton(
    label: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
