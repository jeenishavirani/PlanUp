package com.example.planup.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StreakManager {

    private static final String TAG = "StreakManager";
    private static final String PREF_NAME = "PlanUpStreak";
    private static final String LAST_NOTIFIED_STREAK = "last_notified_streak";

    /**
     * Updates the user's streak in Firestore and notifies if a milestone is reached.
     * Designed to persist streaks even when tasks older than 1 week are deleted.
     */
    public static void updateStreak(Context context, String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(userDoc -> {
            int streakValue = 0;
            if (userDoc.contains("streak")) {
                Object streakObj = userDoc.get("streak");
                if (streakObj instanceof Long) streakValue = ((Long) streakObj).intValue();
                else if (streakObj instanceof Integer) streakValue = (Integer) streakObj;
            }
            // 🔹 This must be final to be used in the nested lambda
            final int currentLifetimeStreak = streakValue;
            final String lastDate = userDoc.getString("lastStreakDate");

            db.collection("users")
                    .document(uid)
                    .collection("tasks")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        Set<String> completedDates = new HashSet<>();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                        String todayStr = sdf.format(new Date());

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String status = doc.getString("status");
                            if ("Completed".equalsIgnoreCase(status) || "Completed Late".equalsIgnoreCase(status)) {
                                Date dueDate = doc.getDate("dueDate");
                                if (dueDate != null) {
                                    completedDates.add(sdf.format(dueDate));
                                }
                            }
                        }

                        int localStreak = calculateLocalStreak(completedDates);
                        int finalStreak = localStreak;

                        // 🔹 Lifetime Streak Persistence Logic
                        if (localStreak >= 7) {
                            if (todayStr.equals(lastDate)) {
                                finalStreak = Math.max(localStreak, currentLifetimeStreak);
                            } else if (isYesterday(lastDate)) {
                                if (completedDates.contains(todayStr)) {
                                    finalStreak = Math.max(localStreak, currentLifetimeStreak + 1);
                                } else {
                                    finalStreak = Math.max(localStreak, currentLifetimeStreak);
                                }
                            } else {
                                finalStreak = localStreak;
                            }
                        } else {
                            finalStreak = localStreak;
                        }

                        // Update Firestore
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("streak", finalStreak);
                        if (completedDates.contains(todayStr)) {
                            updates.put("lastStreakDate", todayStr);
                        }
                        userRef.update(updates);

                        checkAndNotifyMilestone(context, finalStreak);
                    });
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching user for streak", e));
    }

    private static boolean isYesterday(String dateStr) {
        if (dateStr == null) return false;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return dateStr.equals(sdf.format(cal.getTime()));
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

    private static int calculateLocalStreak(Set<String> completedDates) {
        if (completedDates.isEmpty()) return 0;
        int streak = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        
        String todayStr = sdf.format(cal.getTime());
        boolean completedToday = completedDates.contains(todayStr);
        
        cal.add(Calendar.DAY_OF_YEAR, -1);
        boolean completedYesterday = completedDates.contains(sdf.format(cal.getTime()));

        if (!completedToday && !completedYesterday) return 0;

        cal = Calendar.getInstance();
        if (!completedToday) cal.add(Calendar.DAY_OF_YEAR, -1);

        while (completedDates.contains(sdf.format(cal.getTime()))) {
            streak++;
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return streak;
    }
}
