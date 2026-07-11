// TEST DATA — gated behind Settings.devModeEnabled; not removed, see CLEANUP_LOG Pass 21
package com.earnit.app.data

object TestDataSeeder {
    // ── Full dataset ───────────────────────────────────────────────────────────
    // 20 history entries, 100+ archived logs, 300+ total points → all mascots unlock.
    // Active rewards cover every distinct UI state.

    suspend fun seedFull(database: EarnItDatabase) {
        val taskDao = database.taskDao()
        val rewardDao = database.rewardDao()
        val rewardTaskDao = database.rewardTaskCrossRefDao()
        val logDao = database.completionLogDao()
        val historyDao = database.historyDao()

        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val hr = 3_600_000L

        // ── Tasks ──────────────────────────────────────────────────────────────

        val run =
            taskDao.insertTask(
                TaskEntity(name = "Morning Run", points = 3, icon = "🏃", sortOrder = 0, group = "Fitness"),
            )
        val workout =
            taskDao.insertTask(
                TaskEntity(name = "Workout", points = 5, icon = "💪", sortOrder = 1, group = "Fitness"),
            )
        val walk =
            taskDao.insertTask(
                TaskEntity(name = "Evening Walk", points = 2, icon = "🚶", sortOrder = 2, group = "Fitness"),
            )
        // Name at TASK_NAME_MAX_CHARS (56) — layout stress test
        val longTask =
            taskDao.insertTask(
                TaskEntity(
                    name = "Early Morning 5km Run Before Breakfast Then a Cup of Tea",
                    points = 4,
                    icon = "🌅",
                    sortOrder = 3,
                    group = "Fitness",
                ),
            )
        val bigTask =
            taskDao.insertTask(
                TaskEntity(name = "Complete a 10km Race", points = 25, icon = "🏅", sortOrder = 4, group = "Fitness"),
            )
        val read =
            taskDao.insertTask(
                TaskEntity(name = "Read 30 min", points = 2, icon = "📚", sortOrder = 5, group = "Mindfulness"),
            )
        val meditate =
            taskDao.insertTask(
                TaskEntity(name = "Meditate", points = 1, icon = "🧘", sortOrder = 6, group = "Mindfulness"),
            )
        val journal =
            taskDao.insertTask(
                TaskEntity(name = "Journal", points = 2, icon = "✏️", sortOrder = 7, group = "Mindfulness"),
            )
        val cold = taskDao.insertTask(TaskEntity(name = "Cold Shower", points = 3, icon = "🚿", sortOrder = 8))
        val cook =
            taskDao.insertTask(
                TaskEntity(name = "Cook Healthy Meal", points = 3, icon = "🥗", sortOrder = 9, group = "Home"),
            )
        val code =
            taskDao.insertTask(
                TaskEntity(
                    name = "Code Practice",
                    useAutoPoints = true,
                    time = 2,
                    difficulty = 5,
                    preparation = 2,
                    icon = "💻",
                    sortOrder = 10,
                    group = "Skills",
                ),
            )
        val clean =
            taskDao.insertTask(
                TaskEntity(
                    name = "Clean Room",
                    useAutoPoints = true,
                    time = 3,
                    difficulty = 2,
                    preparation = 0,
                    icon = "🧹",
                    sortOrder = 11,
                    group = "Home",
                ),
            )
        val yoga =
            taskDao.insertTask(
                TaskEntity(
                    name = "Yoga Session",
                    useAutoPoints = true,
                    time = 1,
                    difficulty = 3,
                    preparation = 1,
                    icon = "🤸",
                    sortOrder = 12,
                    group = "Mindfulness",
                ),
            )

        // ── History (20 entries, 5 logs each = 100 logs, ~470 pts total) ───────
        // Even entries use high-point tasks; odd entries use lower-point tasks.
        // This guarantees all mascot unlock thresholds are crossed.

        suspend fun history(
            name: String,
            icon: String,
            cost: Int,
            daysAgo: Int,
            logs: List<Triple<Long, String, Int>>,
            notes: List<String> = emptyList(),
        ) {
            val claimedAt = now - daysAgo * day
            val entryId =
                historyDao.insertEntry(
                    HistoryEntryEntity(
                        rewardId = 0L,
                        rewardName = name,
                        rewardIcon = icon,
                        pointCost = cost,
                        claimedAt = claimedAt,
                    ),
                )
            logs.forEachIndexed { i, (tid, tname, pts) ->
                logDao.insertLog(
                    CompletionLogEntity(
                        taskId = tid,
                        taskName = tname,
                        rewardId = 0L,
                        historyEntryId = entryId,
                        timestamp = claimedAt - (logs.size - i) * hr,
                        detail = notes.getOrElse(i) { "" },
                        points = pts,
                    ),
                )
            }
        }

        // High-point set (31 pts × 10 = 310 pts, 50 logs)
        val setA =
            listOf(
                workout to "Workout" to 5,
                run to "Morning Run" to 3,
                code to "Code Practice" to 14,
                yoga to "Yoga Session" to 8,
                meditate to "Meditate" to 1,
            ).map { (p, pts) -> Triple(p.first, p.second, pts) }

        // Lower-point set (16 pts × 10 = 160 pts, 50 logs)
        val setB =
            listOf(
                clean to "Clean Room" to 7,
                walk to "Evening Walk" to 2,
                journal to "Journal" to 2,
                cold to "Cold Shower" to 3,
                read to "Read 30 min" to 2,
            ).map { (p, pts) -> Triple(p.first, p.second, pts) }

        // Note sets — used on alternating entries so some have notes and some don't
        val notesA =
            listOf(
                "Upper body — increased weight on squats",
                "Easy 5k, good pace",
                "Kotlin coroutines deep dive",
                "Morning flow, 45 min",
                "",
            )
        val notesB =
            listOf(
                "Full apartment top to bottom",
                "Sunset loop around the park",
                "Weekly review + goals",
                "",
                "Non-fiction, finished a chapter",
            )

        history("Date Night", "🍷", 30, 365, setA, notesA)
        history("New Book", "📖", 20, 340, setB)
        history("Movie Night", "🎬", 15, 315, setA, notesA)
        history("Weekend Trip", "🏕️", 50, 290, setB, notesB)
        history("New Headphones", "🎧", 35, 265, setA)
        history("Spa Day", "🧖", 40, 240, setB, notesB)
        history("Gaming Session", "🎮", 25, 215, setA, notesA)
        history("Nice Dinner", "🍽️", 25, 190, setB)
        history("New Running Shoes", "👟", 45, 165, setA, notesA)
        history("Coffee Shop Morning", "☕", 15, 140, setB, notesB)
        history("Cinema Trip", "🎬", 20, 115, setA)
        history("Massage", "💆", 30, 95, setB, notesB)
        history("New Shirt", "👕", 20, 80, setA, notesA)
        history("Board Game Night", "🎲", 25, 65, setB)
        history("Yoga Class", "🤸", 20, 55, setA, notesA)
        history("Podcast Marathon", "🎙️", 15, 45, setB, notesB)
        history("Bike Ride Day", "🚴", 30, 35, setA)
        history("Cooking Class", "🍳", 40, 25, setB, notesB)
        history("Concert Tickets", "🎵", 60, 15, setA, notesA)
        history("Tofino Trip", "🏖️", 100, 7, setB, notesB)

        // ── Active Reward 1 — CLAIMABLE (points met + all mandatory done) ──────

        val claimable =
            rewardDao.insertReward(
                RewardEntity(
                    name = "New Trainers",
                    cost = 20,
                    icon = "👟",
                    description = "Earned it — those morning runs paid off.",
                    sortOrder = 0,
                ),
            )
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(claimable, workout, isMandatory = true, isRepeatable = false))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(claimable, run, isMandatory = false, isRepeatable = true))
        logDao.insertLog(
            CompletionLogEntity(
                taskId = workout,
                taskName = "Workout",
                rewardId = claimable,
                timestamp =
                    now - 3 * day,
                detail = "Leg day",
                points = 5,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = run,
                taskName = "Morning Run",
                rewardId = claimable,
                timestamp =
                    now - 2 * day,
                detail = "6k",
                points = 3,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = run,
                taskName = "Morning Run",
                rewardId = claimable,
                timestamp =
                    now - 1 * day,
                detail = "5k PB",
                points = 3,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = run,
                taskName = "Morning Run",
                rewardId = claimable,
                timestamp = now,
                detail = "",
                points = 3,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = run,
                taskName = "Morning Run",
                rewardId = claimable,
                timestamp = now,
                detail = "",
                points = 3,
            ),
        )
        // 17 pts — mandatory done → canClaim = true

        // ── Active Reward 2 — BLOCKED (points met, mandatory NOT logged) ───────

        val blocked = rewardDao.insertReward(RewardEntity(name = "Spa Day", cost = 15, icon = "🧖", sortOrder = 1))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(blocked, meditate, isMandatory = true, isRepeatable = false))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(blocked, walk, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(blocked, yoga, isMandatory = false, isRepeatable = true))
        logDao.insertLog(
            CompletionLogEntity(
                taskId = walk,
                taskName = "Evening Walk",
                rewardId = blocked,
                timestamp =
                    now - 4 * day,
                detail = "",
                points = 2,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = yoga,
                taskName = "Yoga Session",
                rewardId = blocked,
                timestamp =
                    now - 3 * day,
                detail = "",
                points = 8,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = walk,
                taskName = "Evening Walk",
                rewardId = blocked,
                timestamp =
                    now - 2 * day,
                detail = "",
                points = 2,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = yoga,
                taskName = "Yoga Session",
                rewardId = blocked,
                timestamp =
                    now - 1 * day,
                detail = "",
                points = 8,
            ),
        )
        // 20 pts ≥ 15 — but meditate NOT logged → canClaim = false (gatekeeper active)

        // ── Active Reward 3 — ~70% progress, mandatory done ───────────────────

        val progress70 =
            rewardDao.insertReward(
                RewardEntity(name = "Gaming Session", cost = 50, icon = "🎮", sortOrder = 2),
            )
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(progress70, code, isMandatory = true, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(progress70, workout, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(progress70, run, isMandatory = false, isRepeatable = true))
        logDao.insertLog(
            CompletionLogEntity(
                taskId = code,
                taskName = "Code Practice",
                rewardId = progress70,
                timestamp =
                    now - 5 * day,
                detail = "Kotlin flows",
                points = 14,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = workout,
                taskName = "Workout",
                rewardId = progress70,
                timestamp =
                    now - 3 * day,
                detail = "",
                points = 5,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = run,
                taskName = "Morning Run",
                rewardId = progress70,
                timestamp =
                    now - 1 * day,
                detail = "",
                points = 3,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = code,
                taskName = "Code Practice",
                rewardId = progress70,
                timestamp = now,
                detail = "Compose UI",
                points = 14,
            ),
        )
        // 36/50 pts = 72% — mandatory done — not yet claimable

        // ── Active Reward 4 — ~30% progress, no mandatory ─────────────────────

        val progress30 = rewardDao.insertReward(RewardEntity(name = "New Book", cost = 40, icon = "📚", sortOrder = 3))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(progress30, read, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(progress30, journal, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(
            RewardTaskCrossRef(progress30, meditate, isMandatory = false, isRepeatable = false),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = read,
                taskName = "Read 30 min",
                rewardId = progress30,
                timestamp =
                    now - 4 * day,
                detail = "Chapter 3",
                points = 2,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = journal,
                taskName = "Journal",
                rewardId = progress30,
                timestamp =
                    now - 2 * day,
                detail = "",
                points = 2,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = meditate,
                taskName = "Meditate",
                rewardId = progress30,
                timestamp =
                    now - 1 * day,
                detail = "",
                points = 1,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = read,
                taskName = "Read 30 min",
                rewardId = progress30,
                timestamp = now,
                detail = "Fiction",
                points = 2,
            ),
        )
        // 7/40 pts = 17.5% — note: meditate is non-repeatable and already logged → won't appear in log dialog

        // ── Active Reward 5 — zero progress (tasks linked, no logs) ───────────

        val zero = rewardDao.insertReward(RewardEntity(name = "Nice Dinner", cost = 30, icon = "🍽️", sortOrder = 4))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(zero, cook, isMandatory = true, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(zero, workout, isMandatory = false, isRepeatable = true))

        // ── Active Reward 6 — no tasks linked (shows "+ ADD TASKS") ───────────

        rewardDao.insertReward(RewardEntity(name = "Weekend Away", cost = 80, icon = "🏕️", sortOrder = 5))

        // ── Active Reward 7 — name/description at max length + high cost (layout stress test) ──

        val bigReward =
            rewardDao.insertReward(
                RewardEntity(
                    name = "Weekend Hiking Trip to the Rockies Peaks",
                    cost = 500,
                    icon = "⛰️",
                    description =
                        "A multi-day backpacking trip through alpine meadows and rocky ridgelines, camping " +
                            "under the stars each night, with a summit push on the final morning before the " +
                            "long drive home and a well-earned rest.",
                    sortOrder = 6,
                ),
            )
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(bigReward, bigTask, isMandatory = true, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(bigReward, workout, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(bigReward, longTask, isMandatory = false, isRepeatable = true))
        logDao.insertLog(
            CompletionLogEntity(
                taskId = bigTask,
                taskName = "Complete a 10km Race",
                rewardId = bigReward,
                timestamp =
                    now - 7 * day,
                detail = "First official race! 58:32.",
                points = 25,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = workout,
                taskName = "Workout",
                rewardId = bigReward,
                timestamp =
                    now - 3 * day,
                detail = "Cross-training",
                points = 5,
            ),
        )
        // 30/500 pts = 6% — mandatory done — very long road ahead
    }

    // ── Standard dataset ───────────────────────────────────────────────────────

    suspend fun seed(database: EarnItDatabase) {
        val taskDao = database.taskDao()
        val rewardDao = database.rewardDao()
        val rewardTaskDao = database.rewardTaskCrossRefDao()
        val logDao = database.completionLogDao()
        val historyDao = database.historyDao()

        val now = System.currentTimeMillis()
        val day = 86_400_000L

        // ── Tasks ──────────────────────────────────────────────────────────────

        // Groups: Fitness, Mindfulness, Home, Skills — one task ungrouped to show "Other"
        val run =
            taskDao.insertTask(
                TaskEntity(name = "Morning Run", points = 3, icon = "🏃", sortOrder = 0, group = "Fitness"),
            )
        val read =
            taskDao.insertTask(
                TaskEntity(name = "Read 30 min", points = 2, icon = "📚", sortOrder = 1, group = "Mindfulness"),
            )
        val meditate =
            taskDao.insertTask(
                TaskEntity(name = "Meditate", points = 1, icon = "🧘", sortOrder = 2, group = "Mindfulness"),
            )
        val workout =
            taskDao.insertTask(
                TaskEntity(name = "Workout", points = 5, icon = "💪", sortOrder = 3, group = "Fitness"),
            )
        val journal =
            taskDao.insertTask(
                TaskEntity(name = "Journal", points = 2, icon = "✏️", sortOrder = 4, group = "Mindfulness"),
            )
        val walk =
            taskDao.insertTask(
                TaskEntity(name = "Evening Walk", points = 2, icon = "🚶", sortOrder = 5, group = "Fitness"),
            )
        val cold = taskDao.insertTask(TaskEntity(name = "Cold Shower", points = 3, icon = "🚿", sortOrder = 6))
        val cook =
            taskDao.insertTask(
                TaskEntity(name = "Cook Healthy Meal", points = 3, icon = "🥗", sortOrder = 7, group = "Home"),
            )
        val code =
            taskDao.insertTask(
                TaskEntity(
                    name = "Code Practice",
                    useAutoPoints = true,
                    time = 2,
                    difficulty = 5,
                    preparation = 2,
                    icon = "💻",
                    sortOrder = 8,
                    group = "Skills",
                ),
            )
        val clean =
            taskDao.insertTask(
                TaskEntity(
                    name = "Clean Room",
                    useAutoPoints = true,
                    time = 3,
                    difficulty = 2,
                    preparation = 0,
                    icon = "🧹",
                    sortOrder = 9,
                    group = "Home",
                ),
            )
        val yoga =
            taskDao.insertTask(
                TaskEntity(
                    name = "Yoga Session",
                    useAutoPoints = true,
                    time = 1,
                    difficulty = 3,
                    preparation = 1,
                    icon = "🤸",
                    sortOrder = 10,
                    group = "Mindfulness",
                ),
            )
        // Name at TASK_NAME_MAX_CHARS (56) — layout stress test
        val longTask =
            taskDao.insertTask(
                TaskEntity(
                    name = "Early Morning 5km Run Before Breakfast Then a Cup of Tea",
                    points = 4,
                    icon = "🌅",
                    sortOrder = 11,
                    group = "Fitness",
                ),
            )
        // High points — big effort task
        val bigTask =
            taskDao.insertTask(
                TaskEntity(name = "Complete a 10km Race", points = 25, icon = "🏅", sortOrder = 12, group = "Fitness"),
            )

        // ── Active Reward 1 — Tofino Trip ──────────────────────────────────────

        val trip = rewardDao.insertReward(RewardEntity(name = "Tofino Trip", cost = 100, icon = "🏖️", sortOrder = 0))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(trip, workout, isMandatory = true, isRepeatable = false))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(trip, run, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(trip, code, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(trip, yoga, isMandatory = false, isRepeatable = true))

        logDao.insertLog(
            CompletionLogEntity(
                taskId = workout,
                taskName = "Workout",
                rewardId = trip,
                timestamp =
                    now - 12 * day,
                detail = "Leg day — squats, lunges, leg press. Felt strong throughout, upped the weight on squats by 5kg.",
                points = 5,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = run,
                taskName = "Morning Run",
                rewardId = trip,
                timestamp =
                    now - 10 * day,
                detail = "5k",
                points = 3,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = code,
                taskName = "Code Practice",
                rewardId = trip,
                timestamp =
                    now - 8 * day,
                detail = "Kotlin",
                points = 14,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = yoga,
                taskName = "Yoga Session",
                rewardId = trip,
                timestamp =
                    now - 6 * day,
                detail = "Morning flow",
                points = 8,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = run,
                taskName = "Morning Run",
                rewardId = trip,
                timestamp =
                    now - 3 * day,
                detail = "Morning",
                points = 3,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = code,
                taskName = "Code Practice",
                rewardId = trip,
                timestamp =
                    now - 1 * day,
                detail = "Android UI — worked through the Compose layout system, got composable reuse working cleanly.",
                points = 14,
            ),
        )

        // ── Active Reward 2 — New Book ─────────────────────────────────────────

        val book = rewardDao.insertReward(RewardEntity(name = "New Book", cost = 20, icon = "📖", sortOrder = 1))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(book, read, isMandatory = true, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(book, meditate, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(book, journal, isMandatory = false, isRepeatable = false))

        logDao.insertLog(
            CompletionLogEntity(
                taskId = read,
                taskName = "Read 30 min",
                rewardId = book,
                timestamp =
                    now - 5 * day,
                detail = "Philosophy",
                points = 2,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = meditate,
                taskName = "Meditate",
                rewardId = book,
                timestamp =
                    now - 3 * day,
                detail = "",
                points = 1,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = read,
                taskName = "Read 30 min",
                rewardId = book,
                timestamp =
                    now - 1 * day,
                detail = "Finished the first chapter of the Sci-fi novel — really picked up pace by the end, excited to keep going.",
                points = 2,
            ),
        )

        // ── Active Reward 3 — Gaming Session ───────────────────────────────────

        val gaming =
            rewardDao.insertReward(
                RewardEntity(name = "Gaming Session", cost = 30, icon = "🎮", sortOrder = 2),
            )
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(gaming, code, isMandatory = true, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(gaming, workout, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(gaming, cold, isMandatory = false, isRepeatable = false))

        // ── Active Reward 4 — Spa Day ──────────────────────────────────────────

        val spa = rewardDao.insertReward(RewardEntity(name = "Spa Day", cost = 40, icon = "🧖", sortOrder = 3))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(spa, yoga, isMandatory = true, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(spa, walk, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(spa, meditate, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(spa, cold, isMandatory = false, isRepeatable = false))

        logDao.insertLog(
            CompletionLogEntity(
                taskId = yoga,
                taskName = "Yoga Session",
                rewardId = spa,
                timestamp =
                    now - 9 * day,
                detail = "Evening",
                points = 8,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = walk,
                taskName = "Evening Walk",
                rewardId = spa,
                timestamp =
                    now - 7 * day,
                detail = "Park loop — went all the way around the lake, about 4km total. Really helped clear my head after a stressful day.",
                points = 2,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = meditate,
                taskName = "Meditate",
                rewardId = spa,
                timestamp =
                    now - 5 * day,
                detail = "",
                points = 1,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = yoga,
                taskName = "Yoga Session",
                rewardId = spa,
                timestamp =
                    now - 4 * day,
                detail = "Stretch",
                points = 8,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = walk,
                taskName = "Evening Walk",
                rewardId = spa,
                timestamp =
                    now - 2 * day,
                detail = "Waterfront",
                points = 2,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = walk,
                taskName = "Evening Walk",
                rewardId = spa,
                timestamp =
                    now - 1 * day,
                detail = "Riverside path",
                points = 2,
            ),
        )

        // ── Active Reward 5 — Nice Dinner ──────────────────────────────────────

        rewardDao.insertReward(RewardEntity(name = "Nice Dinner", cost = 25, icon = "🍽️", sortOrder = 4))

        // ── Active Reward 6 — name/description at max length (layout stress test) ─────

        val longReward =
            rewardDao.insertReward(
                RewardEntity(
                    name = "Weekend Hiking Trip to the Rockies Peaks",
                    cost = 60,
                    icon = "⛰️",
                    description =
                        "A multi-day backpacking trip through alpine meadows and rocky ridgelines, camping " +
                            "under the stars each night, with a summit push on the final morning before the " +
                            "long drive home and a well-earned rest.",
                    sortOrder = 5,
                ),
            )
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(longReward, longTask, isMandatory = true, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(longReward, run, isMandatory = false, isRepeatable = true))
        logDao.insertLog(
            CompletionLogEntity(
                taskId = longTask,
                taskName = "Early Morning 5km Run Before Breakfast Then a Cup of Tea",
                rewardId = longReward,
                timestamp =
                    now - 2 * day,
                detail = "Out the door by 6am, ran the river trail loop. Legs felt heavy for the first km then loosened up nicely. Spotted a heron near the bridge.",
                points = 4,
            ),
        )

        // ── Active Reward 7 — Big aspirational reward (high cost stress test) ──

        val bigReward =
            rewardDao.insertReward(
                RewardEntity(name = "International Flight", cost = 500, icon = "✈️", sortOrder = 6),
            )
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(bigReward, bigTask, isMandatory = true, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(bigReward, workout, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(bigReward, run, isMandatory = false, isRepeatable = true))
        rewardTaskDao.insertCrossRef(RewardTaskCrossRef(bigReward, code, isMandatory = false, isRepeatable = true))
        logDao.insertLog(
            CompletionLogEntity(
                taskId = bigTask,
                taskName = "Complete a 10km Race",
                rewardId = bigReward,
                timestamp =
                    now - 14 * day,
                detail = "First official race! Finished in 58:32. Paced well for the first 7km then pushed hard on the final stretch. Already signed up for the next one.",
                points = 25,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = workout,
                taskName = "Workout",
                rewardId = bigReward,
                timestamp =
                    now - 4 * day,
                detail = "Cross-training day",
                points = 5,
            ),
        )

        // ── History ────────────────────────────────────────────────────────────

        val hofEntry1 =
            historyDao.insertEntry(
                HistoryEntryEntity(
                    rewardId = 0L,
                    rewardName = "Weekend Trip",
                    rewardIcon = "🏕️",
                    pointCost = 50,
                    claimedAt =
                        now - 60 * day,
                ),
            )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = workout,
                taskName = "Workout",
                rewardId = 0L,
                historyEntryId = hofEntry1,
                timestamp =
                    now - 66 * day,
                detail = "Full body",
                points = 5,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = run,
                taskName = "Morning Run",
                rewardId = 0L,
                historyEntryId = hofEntry1,
                timestamp =
                    now - 64 * day,
                detail = "Rain run",
                points = 3,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = code,
                taskName = "Code Practice",
                rewardId = 0L,
                historyEntryId = hofEntry1,
                timestamp =
                    now - 63 * day,
                detail = "Compose",
                points = 14,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = run,
                taskName = "Morning Run",
                rewardId = 0L,
                historyEntryId = hofEntry1,
                timestamp =
                    now - 62 * day,
                detail = "10k PB",
                points = 3,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = code,
                taskName = "Code Practice",
                rewardId = 0L,
                historyEntryId = hofEntry1,
                timestamp =
                    now - 61 * day,
                detail = "Room DB",
                points = 14,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = yoga,
                taskName = "Yoga Session",
                rewardId = 0L,
                historyEntryId = hofEntry1,
                timestamp =
                    now - 60 * day,
                detail = "Wind-down",
                points = 8,
            ),
        )

        val hofEntry2 =
            historyDao.insertEntry(
                HistoryEntryEntity(
                    rewardId = 0L,
                    rewardName = "New Headphones",
                    rewardIcon = "🎧",
                    pointCost = 35,
                    claimedAt =
                        now - 30 * day,
                ),
            )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = clean,
                taskName = "Clean Room",
                rewardId = 0L,
                historyEntryId = hofEntry2,
                timestamp =
                    now - 36 * day,
                detail = "Deep clean",
                points = 7,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = journal,
                taskName = "Journal",
                rewardId = 0L,
                historyEntryId = hofEntry2,
                timestamp =
                    now - 34 * day,
                detail = "Reflections",
                points = 2,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = workout,
                taskName = "Workout",
                rewardId = 0L,
                historyEntryId = hofEntry2,
                timestamp =
                    now - 33 * day,
                detail = "Push day",
                points = 5,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = walk,
                taskName = "Evening Walk",
                rewardId = 0L,
                historyEntryId = hofEntry2,
                timestamp =
                    now - 31 * day,
                detail = "Sunset",
                points = 2,
            ),
        )
        logDao.insertLog(
            CompletionLogEntity(
                taskId = cold,
                taskName = "Cold Shower",
                rewardId = 0L,
                historyEntryId = hofEntry2,
                timestamp =
                    now - 30 * day,
                detail = "",
                points = 3,
            ),
        )
    }
}
