package com.earnit.app.ui

data class Quote(
    val text: String,
    val source: String = "",
)

internal val motivationalQuotes =
    listOf(
        // Original EarnIt quotes
        Quote("Every point is proof you showed up."),
        Quote("Small wins stack into big rewards."),
        Quote("Progress, not perfection."),
        Quote("Today's effort is tomorrow's reward."),
        Quote("Consistency beats motivation every time."),
        Quote("Another day, another chance to level up."),
        Quote("You're closer than yesterday."),
        Quote("Do it for future you."),
        Quote("Momentum starts with one small step."),
        Quote("Your future self is rooting for you."),
        Quote("Discipline is self-love in disguise."),
        Quote("The only bad workout is the one you skipped."),
        Quote("Show up. Do the work. Collect the glory."),
        // Added quotes
        Quote("Even if you fall on your face, you're still moving forward."),
        Quote("When you reach the end of your rope, tie a knot in it and hang on."),
        Quote(
            "Don't worry about the world coming to an end today. It's already tomorrow in Australia.",
            "Charles Schulz",
        ),
        Quote("He who moves a mountain begins by carrying away small stones."),
        Quote("The wood is already cut, now we just have to carry it."),
        Quote("Even a snail will eventually reach the Ark."),
        Quote("Everything is hard before it is easy."),
        Quote("Life is a shipwreck, but we must not forget to sing in the lifeboats.", "Voltaire"),
        Quote("It does not matter how slowly you go as long as you do not stop.", "Confucius"),
        Quote("Never give up on something that you can't go a day without thinking about.", "Winston Churchill"),
        Quote("As much as talent counts, effort counts twice.", "Angela Duckworth"),
        Quote("I dare to be honest, and I fear no labour.", "Robert Burns"),
        Quote("Life is like riding a bicycle. To keep your balance, you must keep moving.", "Albert Einstein"),
        Quote("Be the change you wish to see in the world.", "Mahatma Gandhi"),
        Quote("You are never too old to set another goal or to dream a new dream.", "C.S. Lewis"),
        Quote("Change your life today. Don't gamble on the future, act now, without delay.", "Simone de Beauvoir"),
        Quote(
            "I may not have gone where I intended to go, but I think I have ended up where I needed to be.",
            "Douglas Adams",
        ),
        Quote("Forever is composed of nows.", "Emily Dickinson"),
        Quote("Today's accomplishments were yesterday's impossibilities.", "Robert H. Schuller"),
        Quote(
            "Someone is sitting in the shade today because someone planted a tree a long time ago.",
            "Warren Buffett",
        ),
        Quote(
            "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            "Winston Churchill",
        ),
        Quote("The most difficult thing is the decision to act; the rest is merely tenacity.", "Amelia Earhart"),
        Quote("You must do the things you think you cannot do.", "Eleanor Roosevelt"),
        Quote("It is never too late to be what you might have been.", "George Eliot"),
    )
