package com.example.planup.ai;

import java.util.ArrayList;
import java.util.List;

public class FollowUpQuestionGenerator {

    public static List<String> getQuestions(String intent) {

        List<String> questions = new ArrayList<>();

        switch (intent) {

            case "EXAM":
                questions.add("📘 Which exam are you preparing for?");
                questions.add("⏳ How many days are left for the exam?");
                questions.add("🕒 How many hours can you study daily?");
                break;

            case "DIET":
                questions.add("🥗 What is your main goal? (weight loss / gain / healthy)");
                questions.add("📅 Do you want a daily or weekly diet plan?");
                questions.add("🚫 Any food restrictions?");
                break;

            case "PREGNANCY":
                questions.add("🤰 Which trimester are you in?");
                questions.add("🕒 Any health conditions we should consider?");
                questions.add("🏃 Do you want a light or detailed routine?");
                break;

            case "FOCUS":
                questions.add("🧠 What distracts you the most?");
                questions.add("⏱ How long can you stay focused currently?");
                questions.add("🎯 What do you want to improve most?");
                break;

            case "WORK":
                questions.add("💼 What kind of work do you do?");
                questions.add("📆 Any deadlines coming soon?");
                questions.add("⚡ Do you want deep focus or balanced schedule?");
                break;

            case "FITNESS":
                questions.add("🏋️ What is your fitness goal?");
                questions.add("📅 How many days per week can you work out?");
                questions.add("🏠 Home or gym workouts?");
                break;

            default:
                questions.add("✨ Tell me more about what you want to achieve.");
                questions.add("⏰ Any deadline or time limit?");
                questions.add("⚡ How intense should the plan be?");
                break;
        }

        return questions;
    }
}
