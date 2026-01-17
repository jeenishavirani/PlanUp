package com.example.planup.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlanGenerator {

    private static final String[] openings = {
            "Alright 😊 let’s do this!",
            "Got you 👍 Here’s a simple plan:",
            "Nice goal 💪 Let’s break it down:",
            "Love this idea ✨ Here’s how you can start:",
            "No worries, I’ve got your back 🤝"
    };

    private static final String[] closings = {
            "You’ve got this 💯",
            "One step at a time 🚶‍♂️",
            "Stay consistent, not perfect ✨",
            "Your future self will thank you 💜",
            "Ping me anytime if you need help 😊"
    };

    public static List<String> generatePlan(String intent, int days, int hoursPerDay) {

        List<String> plan = new ArrayList<>();
        Random random = new Random();

        // 🌟 Friendly opening
        plan.add(openings[random.nextInt(openings.length)]);

        switch (intent) {

            case "EXAM":
                plan.add("📚 Exam Preparation Plan");
                for (int i = 1; i <= days; i++) {
                    plan.add("Day " + i + ": Study for " + hoursPerDay + " hours 🧠");
                }
                break;

            case "DIET":
                plan.add("🥗 Healthy Diet Routine");
                plan.add("• Eat balanced meals 🍎");
                plan.add("• Drink enough water 💧");
                plan.add("• Avoid junk food 🚫");
                break;

            case "FOCUS":
                plan.add("🧠 Focus Improvement Plan");
                plan.add("• Use Pomodoro technique ⏳");
                plan.add("• Reduce distractions 📵");
                plan.add("• Short breaks are important ☕");
                break;

            case "PREGNANCY":
                plan.add("🤰 Gentle Pregnancy Routine");
                plan.add("• Light walking 🚶‍♀️");
                plan.add("• Healthy meals 🥑");
                plan.add("• Enough rest 😴");
                break;

            default:
                plan.add("📝 Personalized Plan");
                plan.add("• Set clear goals 🎯");
                plan.add("• Break tasks into steps ✂️");
                plan.add("• Review progress daily 📊");
                break;
        }

        // 💬 Friendly closing
        plan.add(closings[random.nextInt(closings.length)]);

        return plan;
    }
}
