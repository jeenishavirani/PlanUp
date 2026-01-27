package com.example.planup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.planup.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;

public class ReminderReceiver extends BroadcastReceiver {

    private final String[] morningMessages = {
            "🌞 Fresh start! A new day, a new chance 💜",
            "Good morning ☀️ One small task can change your day",
            "New day. New energy. Let’s go 🚀",
            "Start slow, stay consistent 🌱",
            "You’ve got this 💪 Let’s make today count"
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("type");
        String taskTitle = intent.getStringExtra("taskTitle");

        if (type != null) {
            if ("TWO_HOUR".equals(type)) {
                NotificationHelper.showNotification(context, "⏳ Task coming up", "In 2 hours: " + taskTitle);
            } else if ("FIVE_MIN".equals(type)) {
                NotificationHelper.showNotification(context, "🚨 Almost time!", "In 5 minutes: " + taskTitle);
            } else if ("MISSED".equals(type)) {
                String taskIdStr = intent.getStringExtra("taskIdStr");
                updateStatusToMissedAndNotify(context, taskIdStr, taskTitle);
            }
            return;
        }

        // Default Morning Reminder
        String message = morningMessages[new Random().nextInt(morningMessages.length)];
        NotificationHelper.showNotification(context, "PlanUp 🌅", message);
    }

    private void updateStatusToMissedAndNotify(Context context, String taskId, String taskTitle) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null || taskId == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        // 1. Check current status
        db.collection("users").document(uid).collection("tasks").document(taskId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        
                        // 2. Only if still "Pending", update to "Missed" and show notification
                        if ("Pending".equals(status)) {
                            db.collection("users").document(uid).collection("tasks").document(taskId)
                                    .update("status", "Missed")
                                    .addOnSuccessListener(unused -> {
                                        Log.d("ReminderReceiver", "Task status updated to Missed: " + taskId);

                                        NotificationHelper.showNotification(
                                                context,
                                                "⚠️ Task Missed",
                                                "You missed: " + taskTitle + ". The task is now marked as Missed."
                                        );
                                    })
                                    .addOnFailureListener(e -> Log.e("ReminderReceiver", "Failed to update status to Missed", e));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ReminderReceiver", "Error checking task status", e));
    }
}
