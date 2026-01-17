package com.example.planup.ai;

import java.util.List;

public class PlannerEngine {

    public static String generatePlan(
            String intent,
            List<String> userAnswers
    ) {

        StringBuilder plan = new StringBuilder();

        switch (intent) {

            case "EXAM":
                plan.append("📚 Personalized Exam Study Plan\n\n");
                plan.append("🎯 Exam: ").append(userAnswers.get(0)).append("\n");
                plan.append("📅 Duration: ").append(userAnswers.get(1)).append("\n");
                plan.append("⏰ Daily Study: ").append(userAnswers.get(2)).append("\n\n");

                plan.append("🗓 Daily Structure:\n");
                plan.append("• Morning: New concepts\n");
                plan.append("• Afternoon: Practice questions\n");
                plan.append("• Evening: Revision\n");
                plan.append("• Night: Light review\n\n");

                plan.append("💡 Tips:\n");
                plan.append("• Revise weekly\n");
                plan.append("• Take 1 mock test every 7 days\n");
                plan.append("• Sleep well and stay consistent\n");
                break;

            case "DIET":
                plan.append("🥗 Personalized Diet Routine\n\n");
                plan.append("🎯 Goal: ").append(userAnswers.get(0)).append("\n");
                plan.append("📆 Plan Type: ").append(userAnswers.get(1)).append("\n");
                plan.append("🚫 Restrictions: ").append(userAnswers.get(2)).append("\n\n");

                plan.append("🍽 Daily Structure:\n");
                plan.append("• Morning: Light breakfast\n");
                plan.append("• Lunch: Balanced meal\n");
                plan.append("• Evening: Fruits / nuts\n");
                plan.append("• Dinner: Light & protein rich\n");
                break;

            case "FOCUS":
                plan.append("🧠 Focus Improvement Plan\n\n");
                plan.append("• Work in 25-minute focus blocks\n");
                plan.append("• 5-minute breaks\n");
                plan.append("• Phone away during sessions\n");
                plan.append("• Review productivity nightly\n");
                break;

            case "PREGNANCY":
                plan.append("🤰 Gentle Pregnancy Routine\n\n");
                plan.append("• Light exercises\n");
                plan.append("• Proper hydration\n");
                plan.append("• Rest breaks\n");
                plan.append("• Doctor-friendly schedule\n");
                break;

            default:
                plan.append("✨ Personalized Routine\n\n");
                plan.append("• Clear goals\n");
                plan.append("• Time-blocking\n");
                plan.append("• Regular breaks\n");
                plan.append("• Daily review\n");
                break;
        }

        plan.append("\n🌱 Remember: Consistency beats intensity.");

        return plan.toString();
    }
}
