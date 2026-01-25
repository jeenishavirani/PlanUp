package com.example.planup.utils;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TaskActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String taskId = intent.getStringExtra("taskId");

        if ("ACTION_DONE".equals(action) && taskId != null) {
            markTaskAsDone(context, taskId);
        }
    }

    private void markTaskAsDone(Context context, String taskId) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .document(taskId)
                .update("status", "Completed")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(context, "Task marked as completed!", Toast.LENGTH_SHORT).show();

                    // Cancel the notification
                    NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(taskId.hashCode());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to update task", Toast.LENGTH_SHORT).show());
    }
}