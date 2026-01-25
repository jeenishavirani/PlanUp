package com.example.planup.utils;

import java.util.Random;

public class NotificationMessages {

    private static final Random random = new Random();

    // 🔔 Task Reminder Messages
    private static final String[] REMINDER_MESSAGES = {
            "Hey 👋 just a reminder — you’ve got a task waiting!",
            "⏰ Don’t forget! A task needs your attention.",
            "One small task now = peace later ✨",
            "You’re closer than you think. Let’s finish this 💪",
            "Quick check-in 🚀 You’ve got this task!"
    };

    // ❌ Missed Task Messages
    private static final String[] MISSED_MESSAGES = {
            "It’s okay 💜 You missed a task — let’s try again!",
            "No stress. One missed task doesn’t define you 🌱",
            "Yesterday slipped. Today is still yours ✨",
            "Missed it? No worries — progress continues 🚶‍♂️",
            "Every comeback starts now 💪"
    };

    // 🌅 Daily Motivation Messages
    private static final String[] DAILY_MESSAGES = {
            "Good morning ☀️ Let’s make today productive!",
            "New day, fresh energy 🌱",
            "Small wins today build big success tomorrow 🔥",
            "Focus on progress, not perfection 💜",
            "One task at a time — you’ve got this 💯"
    };

    // 🔹 Helpers
    public static String getReminderMessage() {
        return REMINDER_MESSAGES[random.nextInt(REMINDER_MESSAGES.length)];
    }

    public static String getMissedMessage() {
        return MISSED_MESSAGES[random.nextInt(MISSED_MESSAGES.length)];
    }

    public static String getDailyMessage() {
        return DAILY_MESSAGES[random.nextInt(DAILY_MESSAGES.length)];
    }
}