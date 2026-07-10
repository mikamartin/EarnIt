package com.earnit.app.data

enum class AppColorScheme { WARM_GOLD, OCEAN_BLUE, FOREST }

data class AppSettings(
    val colorScheme: AppColorScheme = AppColorScheme.WARM_GOLD,
    val notesMandatory: Boolean = false,
    val optimalRewardCount: Int = 3,
    val maxRewardCount: Int = 7,
    val nickname: String = "Babe",
    val selectedMascotId: MascotId? = MascotId.PUGSLY,
    val unlockedMascotIds: Set<MascotId> = setOf(MascotId.PUGSLY, MascotId.TABBY),
    val showQuote: Boolean = true,
    val useRandomNickname: Boolean = false,
    val tasksGroupView: Boolean = false,
    val devModeEnabled: Boolean = false,
    val widgetNudgeDismissed: Boolean = false,
    val settingsTipDismissed: Boolean = false,
    val nudgeStage: Int = 0,
    val nudgeAnchorTimestamp: Long = 0L,
)
