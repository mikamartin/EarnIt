package com.earnit.app.ui

// Pure field-input transforms shared by every Compose text field that caps length or
// filters to digits, extracted from onValueChange blocks so they're testable without Compose.

fun acceptWithinLimit(
    current: String,
    incoming: String,
    max: Int,
): String = if (incoming.length <= max) incoming else current

fun String.digitsOnly(): String = filter { it.isDigit() }

data class NicknameFieldEdit(
    val text: String,
    val shouldDisableRandomNickname: Boolean,
)

// Caps the nickname field, then signals whether the accepted edit should turn off the
// "random nickname" toggle — typing a name is treated as opting back into it.
fun nicknameFieldEdit(
    current: String,
    incoming: String,
    useRandomNickname: Boolean,
): NicknameFieldEdit {
    val next = acceptWithinLimit(current, incoming, NICKNAME_MAX_CHARS)
    return NicknameFieldEdit(next, shouldDisableRandomNickname = next == incoming && useRandomNickname)
}

data class TaskGroupFieldEdit(
    val text: String,
    val shouldClearSelectedGroup: Boolean,
)

// Caps the new-group text field, then signals whether the accepted edit should clear the
// existing-group selection — typing a new group name deselects any picked existing group.
fun taskGroupFieldEdit(
    current: String,
    incoming: String,
): TaskGroupFieldEdit {
    val next = acceptWithinLimit(current, incoming, TASK_GROUP_MAX_CHARS)
    return TaskGroupFieldEdit(next, shouldClearSelectedGroup = next.isNotBlank())
}

// Shared shape behind TaskEditScreen's and RewardEditScreen's name-conflict check: case- and
// whitespace-insensitive match against every other item's name, skipped while a save navigation
// is already pending so the error doesn't flash after a successful save.
fun isDuplicateName(
    candidateName: String,
    existingNames: List<Pair<Long, String>>,
    selfId: Long,
    navPending: Boolean,
): Boolean =
    !navPending &&
        candidateName.isNotBlank() &&
        existingNames.any { (id, existingName) ->
            existingName.trim().equals(candidateName.trim(), ignoreCase = true) && id != selfId
        }
