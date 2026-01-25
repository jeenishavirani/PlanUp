package com.example.planup.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StreakManager {

    private static final String TAG = "StreakManager";
    private static final String PREF_NAME = "PlanUpStreak";
    private static final String LAST_NOTIFIED_STREAK = "last_notified_streak";

    /**
     * Updates the user's streak in Firestore and notifies if a milestone is reached.
     * Uses java.time for local timezone correctness and avoids string comparisons.
     */
    public static void updateStreak(Context context, String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(userDoc -> {
            int currentLifetimeStreak = 0;
            if (userDoc.exists() && userDoc.contains("streak")) {
                Long streakLong = userDoc.getLong("streak");
                if (streakLong != null) currentLifetimeStreak = streakLong.intValue();
            }

            final int finalLifetimeStreak = currentLifetimeStreak;
            final String lastDateStr = userDoc.getString("lastStreakDate");

            db.collection("users")
                    .document(uid)
                    .collection("tasks")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        ZoneId zoneId = ZoneId.systemDefault();
                        LocalDate today = LocalDate.now(zoneId);
                        Set<LocalDate> completedDates = new HashSet<>();

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String status = doc.getString("status");
                            if ("Completed".equalsIgnoreCase(status) || "Completed Late".equalsIgnoreCase(status)) {
                                Long completedAt = doc.getLong("completedAt");
                                if (completedAt != null) {
                                    completedDates.add(Instant.ofEpochMilli(completedAt)
                                            .atZone(zoneId)
                                            .toLocalDate());
                                }
                            }
                        }

                        int localStreak = calculateLocalStreak(completedDates, today);
                        int streakToSave = localStreak;

                        // Lifetime Streak Persistence Logic
                        if (lastDateStr != null) {
                            try {
                                LocalDate lastDate = LocalDate.parse(lastDateStr);
                                if (today.isEqual(lastDate)) {
                                    streakToSave = Math.max(localStreak, finalLifetimeStreak);
                                } else if (today.minusDays(1).isEqual(lastDate)) {
                                    if (completedDates.contains(today)) {
                                        streakToSave = Math.max(localStreak, finalLifetimeStreak + 1);
                                    } else {
                                        streakToSave = Math.max(localStreak, finalLifetimeStreak);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing lastStreakDate", e);
                            }
                        }

                        // Update Firestore
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("streak", streakToSave);
                        if (completedDates.contains(today)) {
                            updates.put("lastStreakDate", today.toString());
                        }
                        userRef.update(updates);

                        checkAndNotifyMilestone(context, streakToSave);
                    });
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching user for streak", e));
    }

    private static int calculateLocalStreak(Set<LocalDate> completedDates, LocalDate today) {
        if (completedDates.isEmpty()) return 0;

        int streak = 0;
        boolean completedToday = completedDates.contains(today);
        boolean completedYesterday = completedDates.contains(today.minusDays(1));

        if (!completedToday && !completedYesterday) return 0;

        LocalDate checkDate = completedToday ? today : today.minusDays(1);
        while (completedDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }
        return streak;
    }

    private static void checkAndNotifyMilestone(Context context, int streak) {
        if (streak <= 0) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int lastNotified = prefs.getInt(LAST_NOTIFIED_STREAK, 0);

        if (streak > lastNotified) {
            String message = "";
            if (streak == 1) message = "You started a new streak! Keep it going.";
            else if (streak == 3) message = "3 days in a row! You're building a great habit.";
            else if (streak == 7) message = "One week strong! You're unstoppable. 🚀";
            else if (streak % 5 == 0) message = streak + " days of consistency! Your future self will thank you.";

            if (!message.isEmpty()) {
                NotificationHelper.showMilestoneNotification(context, "Productivity Streak! 🔥", message);
                prefs.edit().putInt(LAST_NOTIFIED_STREAK, streak).apply();
            }
        }
    }
}