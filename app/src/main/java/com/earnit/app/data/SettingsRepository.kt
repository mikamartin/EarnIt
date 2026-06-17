package com.earnit.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "earnit_settings")

@Singleton
class SettingsRepository
    @Inject
    constructor(
        private val context: Context,
    ) {
        private object Keys {
            val COLOR_SCHEME = stringPreferencesKey("color_scheme")
            val NOTES_MANDATORY = booleanPreferencesKey("notes_mandatory")
            val OPTIMAL_REWARD_COUNT = intPreferencesKey("optimal_reward_count")
            val MAX_REWARD_COUNT = intPreferencesKey("max_reward_count")
            val NICKNAME = stringPreferencesKey("nickname")
            val SHOW_PUGSLY = booleanPreferencesKey("show_pugsly") // legacy — read only for migration
            val SELECTED_MASCOT_ID = stringPreferencesKey("selected_mascot_id")
            val UNLOCKED_MASCOT_IDS = stringPreferencesKey("unlocked_mascot_ids")
            val SHOW_QUOTE = booleanPreferencesKey("show_quote")
            val USE_RANDOM_NICKNAME = booleanPreferencesKey("use_random_nickname")
            val TASKS_GROUP_VIEW = booleanPreferencesKey("tasks_group_view")
            val DEV_MODE_ENABLED = booleanPreferencesKey("dev_mode_enabled")
        }

        val settings: Flow<AppSettings> =
            context.dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { prefs ->
                    // Key never written → legacy migration; "" → explicit hidden; name → enum value
                    val selectedMascotId: MascotId? =
                        when (val raw = prefs[Keys.SELECTED_MASCOT_ID]) {
                            null -> if (prefs[Keys.SHOW_PUGSLY] != false) MascotId.PUGSLY else null
                            "" -> null
                            else -> runCatching { MascotId.valueOf(raw) }.getOrNull()
                        }

                    val unlockedMascotIds: Set<MascotId> =
                        prefs[Keys.UNLOCKED_MASCOT_IDS]
                            ?.split(",")
                            ?.filter { it.isNotEmpty() }
                            ?.mapNotNull { runCatching { MascotId.valueOf(it) }.getOrNull() }
                            ?.toSet()
                            ?: setOf(MascotId.PUGSLY, MascotId.TABBY)

                    AppSettings(
                        colorScheme =
                            prefs[Keys.COLOR_SCHEME]?.let {
                                runCatching { AppColorScheme.valueOf(it) }.getOrDefault(AppColorScheme.WARM_GOLD)
                            } ?: AppColorScheme.WARM_GOLD,
                        notesMandatory = prefs[Keys.NOTES_MANDATORY] ?: false,
                        optimalRewardCount = prefs[Keys.OPTIMAL_REWARD_COUNT] ?: 3,
                        maxRewardCount = prefs[Keys.MAX_REWARD_COUNT] ?: 7,
                        nickname = prefs[Keys.NICKNAME] ?: "Babe",
                        selectedMascotId = selectedMascotId,
                        unlockedMascotIds = unlockedMascotIds,
                        showQuote = prefs[Keys.SHOW_QUOTE] ?: true,
                        useRandomNickname = prefs[Keys.USE_RANDOM_NICKNAME] ?: false,
                        tasksGroupView = prefs[Keys.TASKS_GROUP_VIEW] ?: false,
                        devModeEnabled = prefs[Keys.DEV_MODE_ENABLED] ?: false,
                    )
                }

        suspend fun updateColorScheme(scheme: AppColorScheme) {
            context.dataStore.edit { it[Keys.COLOR_SCHEME] = scheme.name }
        }

        suspend fun updateNotesMandatory(mandatory: Boolean) {
            context.dataStore.edit { it[Keys.NOTES_MANDATORY] = mandatory }
        }

        suspend fun updateOptimalRewardCount(count: Int) {
            context.dataStore.edit { it[Keys.OPTIMAL_REWARD_COUNT] = count }
        }

        suspend fun updateMaxRewardCount(count: Int) {
            context.dataStore.edit { it[Keys.MAX_REWARD_COUNT] = count }
        }

        suspend fun updateNickname(name: String) {
            context.dataStore.edit { it[Keys.NICKNAME] = name }
        }

        suspend fun updateSelectedMascot(id: MascotId?) {
            context.dataStore.edit { it[Keys.SELECTED_MASCOT_ID] = id?.name ?: "" }
        }

        suspend fun updateUnlockedMascots(ids: Set<MascotId>) {
            context.dataStore.edit { it[Keys.UNLOCKED_MASCOT_IDS] = ids.joinToString(",") { it.name } }
        }

        suspend fun updateShowQuote(show: Boolean) {
            context.dataStore.edit { it[Keys.SHOW_QUOTE] = show }
        }

        suspend fun updateUseRandomNickname(use: Boolean) {
            context.dataStore.edit { it[Keys.USE_RANDOM_NICKNAME] = use }
        }

        suspend fun updateTasksGroupView(groupView: Boolean) {
            context.dataStore.edit { it[Keys.TASKS_GROUP_VIEW] = groupView }
        }

        suspend fun enableDevMode() {
            context.dataStore.edit { it[Keys.DEV_MODE_ENABLED] = true }
        }

        suspend fun disableDevMode() {
            context.dataStore.edit { it[Keys.DEV_MODE_ENABLED] = false }
        }
    }
