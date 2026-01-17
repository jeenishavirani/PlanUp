package com.example.planup.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.planup.R;

public class NotificationHelper {

    // 🔹 PUBLIC so receivers & helpers can access it
    public static final String CHANNEL_ID = "planup_reminders";

    // 🔹 OPTIONAL INIT (safe to call from MainActivity / Application)
    public static void init(Context context) {
        createChannel(context);
    }

    // 🔹 SHORT METHOD (used by HomeFragment earlier)
    public static void show(Context context, String title, String message) {
        showNotification(context, title, message);
    }

    // 🔹 MAIN METHOD (receivers should call this)
    public static void showNotification(Context context, String title, String message) {

        createChannel(context);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    // 🔹 CHANNEL CREATION (called internally, safe multiple times)
    private static void createChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "PlanUp Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Friendly reminders from PlanUp");

            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
