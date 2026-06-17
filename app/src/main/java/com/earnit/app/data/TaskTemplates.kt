// Task templates for quick-start sets — imported from Settings
package com.earnit.app.data

data class TaskTemplate(
    val name: String,
    val icon: String,
    val tasks: List<TemplateTask>,
)

data class TemplateTask(
    val name: String,
    val points: Int,
    val icon: String,
)

val taskTemplates =
    listOf(
        TaskTemplate(
            name = "Healthy Living",
            icon = "🌿",
            tasks =
                listOf(
                    TemplateTask("Morning Run", 3, "🏃"),
                    TemplateTask("Workout", 5, "💪"),
                    TemplateTask("Yoga Session", 3, "🤸"),
                    TemplateTask("Meditate", 1, "🧘"),
                    TemplateTask("Cold Shower", 3, "🚿"),
                    TemplateTask("Cook Healthy Meal", 3, "🥗"),
                    TemplateTask("Drink 2L Water", 1, "💧"),
                    TemplateTask("Sleep 8 Hours", 2, "😴"),
                    TemplateTask("Evening Walk", 2, "🚶"),
                    TemplateTask("Stretch", 1, "🦵"),
                ),
        ),
        TaskTemplate(
            name = "Social",
            icon = "🤝",
            tasks =
                listOf(
                    TemplateTask("Call a Friend", 2, "📞"),
                    TemplateTask("Family Dinner", 3, "👨‍👩‍👧"),
                    TemplateTask("Help Someone", 3, "🤝"),
                    TemplateTask("Send a Thank You", 1, "💌"),
                    TemplateTask("Plan a Social Event", 4, "🎉"),
                    TemplateTask("Volunteer", 5, "❤️"),
                    TemplateTask("Write a Letter", 2, "✉️"),
                    TemplateTask("Check In on Someone", 1, "💬"),
                    TemplateTask("Host a Friend", 4, "🏠"),
                    TemplateTask("Join a Club or Group", 3, "🙌"),
                ),
        ),
        TaskTemplate(
            name = "Clean Home",
            icon = "🏠",
            tasks =
                listOf(
                    TemplateTask("Clean Room", 3, "🧹"),
                    TemplateTask("Do Laundry", 2, "👕"),
                    TemplateTask("Wash Dishes", 1, "🍽️"),
                    TemplateTask("Vacuum", 2, "🌀"),
                    TemplateTask("Clean Bathroom", 3, "🚿"),
                    TemplateTask("Take Out Trash", 1, "🗑️"),
                    TemplateTask("Organize a Drawer", 2, "📦"),
                    TemplateTask("Wipe Down Surfaces", 1, "🧽"),
                    TemplateTask("Mop Floors", 2, "🪣"),
                    TemplateTask("Declutter", 3, "✨"),
                ),
        ),
        TaskTemplate(
            name = "Social for Introverts",
            icon = "🌙",
            tasks =
                listOf(
                    TemplateTask("One-on-one coffee date", 3, "☕"),
                    TemplateTask("Text someone you miss", 1, "💬"),
                    TemplateTask("Write a meaningful message", 2, "💌"),
                    TemplateTask("Watch something with a friend", 2, "📺"),
                    TemplateTask("Reach out to an old contact", 2, "📱"),
                    TemplateTask("Reply to avoided messages", 1, "✉️"),
                    TemplateTask("Plan a small gathering", 3, "🏡"),
                    TemplateTask("Have a deep conversation", 2, "💭"),
                    TemplateTask("Chat with a neighbour", 1, "👋"),
                ),
        ),
        TaskTemplate(
            name = "Social for Gamers",
            icon = "🎮",
            tasks =
                listOf(
                    TemplateTask("Online session with friends", 2, "🎮"),
                    TemplateTask("Host a game night", 4, "🎲"),
                    TemplateTask("Teach someone a game you love", 3, "🎓"),
                    TemplateTask("Try a co-op game", 2, "🤝"),
                    TemplateTask("Join a gaming community", 2, "🌐"),
                    TemplateTask("Recommend a game", 1, "⭐"),
                    TemplateTask("Try a friend's favourite game", 2, "🎯"),
                    TemplateTask("Finish a game together", 3, "🏆"),
                    TemplateTask("Watch a friend stream", 1, "📺"),
                    TemplateTask("Talk on voice chat", 1, "🎙️"),
                    TemplateTask("Send a friend request to someone new", 1, "➕"),
                    TemplateTask("Invite someone in-game to group up", 2, "👥"),
                ),
        ),
        TaskTemplate(
            name = "Outdoors for Indoor Dwellers",
            icon = "🌿",
            tasks =
                listOf(
                    TemplateTask("10-min walk outside", 2, "🚶"),
                    TemplateTask("Sit outside with a coffee", 1, "☕"),
                    TemplateTask("Visit a park or green space", 2, "🌳"),
                    TemplateTask("Photograph something in nature", 2, "📷"),
                    TemplateTask("Eat a meal outside", 1, "🌤️"),
                    TemplateTask("Watch the sunrise or sunset", 2, "🌅"),
                    TemplateTask("Take the long route", 1, "🗺️"),
                    TemplateTask("Tend a plant or garden", 2, "🪴"),
                    TemplateTask("Open the window for fresh air", 1, "🪟"),
                    TemplateTask("Walk somewhere you'd usually drive", 3, "🚶"),
                ),
        ),
        TaskTemplate(
            name = "Money Habits",
            icon = "💰",
            tasks =
                listOf(
                    TemplateTask("Check your balance", 1, "💳"),
                    TemplateTask("Review your subscriptions", 2, "📋"),
                    TemplateTask("Transfer to savings", 3, "🏦"),
                    TemplateTask("Track today's spending", 1, "📊"),
                    TemplateTask("Set a weekly budget", 2, "📅"),
                    TemplateTask("Cancel something unused", 3, "✂️"),
                    TemplateTask("Research one investment", 3, "📈"),
                    TemplateTask("Make a savings goal", 2, "🎯"),
                    TemplateTask("Sell something you don't use", 3, "💸"),
                    TemplateTask("Read about personal finance", 2, "📚"),
                ),
        ),
        TaskTemplate(
            name = "Make Parents Happy",
            icon = "❤️",
            tasks =
                listOf(
                    TemplateTask("Call your parents", 2, "📞"),
                    TemplateTask("Visit your parents", 4, "🏡"),
                    TemplateTask("Cook for them or with them", 3, "🍳"),
                    TemplateTask("Help with something at their home", 3, "🔧"),
                    TemplateTask("Share a meal together", 2, "🍽️"),
                    TemplateTask("Send them photos from your week", 1, "📸"),
                    TemplateTask("Plan a family day out", 4, "📅"),
                    TemplateTask("Watch a show together", 2, "📺"),
                    TemplateTask("Ask how they're really doing", 1, "💬"),
                    TemplateTask("Remember an important date", 2, "🎂"),
                ),
        ),
        TaskTemplate(
            name = "Make Parents Happy · Teen",
            icon = "❤️",
            tasks =
                listOf(
                    TemplateTask("Do the dishes without being asked", 2, "🍽️"),
                    TemplateTask("Tidy your room properly", 2, "🧹"),
                    TemplateTask("Put your laundry away", 1, "👕"),
                    TemplateTask("Help with groceries", 2, "🛍️"),
                    TemplateTask("Ask how their day was", 1, "💬"),
                    TemplateTask("Phone away at dinner", 2, "📵"),
                    TemplateTask("Do a chore unprompted", 3, "✨"),
                    TemplateTask("Spend an evening with the family", 2, "🛋️"),
                    TemplateTask("Tell them about your day", 1, "💭"),
                    TemplateTask("Cook a meal for the family", 4, "🍳"),
                ),
        ),
    )
