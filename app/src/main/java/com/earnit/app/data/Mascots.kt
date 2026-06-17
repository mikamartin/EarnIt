package com.earnit.app.data

import com.earnit.app.R

enum class MascotId {
    PUGSLY,
    TABBY,
    MASCOT_3,
    MASCOT_4,
    MASCOT_5,
    MASCOT_6,
    MASCOT_7,
    MASCOT_8,
    MASCOT_9,
}

sealed class UnlockCondition {
    object Always : UnlockCondition()

    data class ClaimsReached(
        val count: Int,
    ) : UnlockCondition()

    data class PointsReached(
        val total: Int,
    ) : UnlockCondition()

    data class TasksCompleted(
        val total: Int,
    ) : UnlockCondition()
}

data class MascotDef(
    val id: MascotId,
    val displayName: String,
    val drawable: Int?,
    val unlockCondition: UnlockCondition,
    val unlockHint: String,
)

object Mascots {
    val all: List<MascotDef> =
        listOf(
            MascotDef(
                id = MascotId.PUGSLY,
                displayName = "Pugsly",
                drawable = R.drawable.pugsly,
                unlockCondition = UnlockCondition.Always,
                unlockHint = "",
            ),
            MascotDef(
                id = MascotId.TABBY,
                displayName = "Tabby",
                drawable = R.drawable.tabby,
                unlockCondition = UnlockCondition.Always,
                unlockHint = "",
            ),
            MascotDef(
                id = MascotId.MASCOT_3,
                displayName = "Panda",
                drawable = R.drawable.panda,
                unlockCondition = UnlockCondition.ClaimsReached(1),
                unlockHint = "Claim your 1st reward",
            ),
            MascotDef(
                id = MascotId.MASCOT_4,
                displayName = "Penguin",
                drawable = R.drawable.penguin,
                unlockCondition = UnlockCondition.PointsReached(100),
                unlockHint = "Earn 100 total points",
            ),
            MascotDef(
                id = MascotId.MASCOT_5,
                displayName = "Otter",
                drawable = R.drawable.otter,
                unlockCondition = UnlockCondition.TasksCompleted(25),
                unlockHint = "Complete 25 tasks",
            ),
            MascotDef(
                id = MascotId.MASCOT_6,
                displayName = "Capybara",
                drawable = R.drawable.capybara,
                unlockCondition = UnlockCondition.ClaimsReached(5),
                unlockHint = "Claim 5 rewards",
            ),
            MascotDef(
                id = MascotId.MASCOT_7,
                displayName = "Thunderbird",
                drawable = R.drawable.thunderbird,
                unlockCondition = UnlockCondition.PointsReached(300),
                unlockHint = "Earn 300 total points",
            ),
            MascotDef(
                id = MascotId.MASCOT_8,
                displayName = "Qilin",
                drawable = R.drawable.qilin,
                unlockCondition = UnlockCondition.TasksCompleted(100),
                unlockHint = "Complete 100 tasks",
            ),
            MascotDef(
                id = MascotId.MASCOT_9,
                displayName = "Dragon",
                drawable = R.drawable.dragon,
                unlockCondition = UnlockCondition.ClaimsReached(20),
                unlockHint = "Claim 20 rewards",
            ),
        )

    fun computeNewlyUnlocked(
        totalClaims: Int,
        totalPoints: Int,
        totalTasks: Int,
        alreadyUnlocked: Set<MascotId>,
    ): List<MascotId> =
        all
            .filter { it.id !in alreadyUnlocked }
            .filter { mascot ->
                when (val c = mascot.unlockCondition) {
                    is UnlockCondition.Always -> true
                    is UnlockCondition.ClaimsReached -> totalClaims >= c.count
                    is UnlockCondition.PointsReached -> totalPoints >= c.total
                    is UnlockCondition.TasksCompleted -> totalTasks >= c.total
                }
            }.map { it.id }
}
