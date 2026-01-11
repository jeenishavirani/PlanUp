package com.example.planup;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.planup.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class PendingTaskReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot == null || snapshot.isEmpty()) return;

                    int pendingCount = snapshot.size();

                    NotificationCompat.Builder builder =
                            new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_notification)
                                    .setContentTitle("⏰ Pending tasks reminder")
                                    .setContentText(
                                            "You have " + pendingCount +
                                                    " pending task" + (pendingCount > 1 ? "s" : "") +
                                                    ". Let’s get them done 💪"
                                    )
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setAutoCancel(true);

                    NotificationManager manager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                    if (manager != null) {
                        manager.notify(2001, builder.build());
                    }
                });
    }
}
