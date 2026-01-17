package com.example.planup.ai;

import java.util.Locale;

public class IntentDetector {

    public static String detect(String userInput) {

        if (userInput == null || userInput.trim().isEmpty()) {
            return "CUSTOM";
        }

        String text = userInput.toLowerCase(Locale.US);

        // 🎓 Exam / Study
        if (containsAny(text,
                "exam", "study", "revision", "syllabus",
                "test", "college", "school", "board", "jee", "neet")) {
            return "EXAM";
        }

        // 🥗 Diet / Health
        if (containsAny(text,
                "diet", "food", "nutrition", "weight",
                "meal", "healthy", "fat loss", "calorie")) {
            return "DIET";
        }

        // 🤰 Pregnancy
        if (containsAny(text,
                "pregnant", "pregnancy", "baby",
                "trimester", "delivery", "mother")) {
            return "PREGNANCY";
        }

        // 🧠 Focus / Productivity
        if (containsAny(text,
                "focus", "concentration", "productive",
                "attention", "lazy", "distracted")) {
            return "FOCUS";
        }

        // 💼 Work / Job
        if (containsAny(text,
                "work", "job", "office", "career",
                "project", "deadline")) {
            return "WORK";
        }

        // 🏋️ Fitness
        if (containsAny(text,
                "fitness", "workout", "gym",
                "exercise", "training", "yoga")) {
            return "FITNESS";
        }

        // ✍️ Anything else
        return "CUSTOM";
    }

    // 🔍 Helper function
    private static boolean containsAny(String text, String... keywords) {
        for (String word : keywords) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
