package com.example.planup.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.planup.MainActivity;
import com.example.planup.R;

public class NotificationHelper {

    public static final String CHANNEL_ID = "planup_reminders";
    public static final String CHANNEL_MILESTONES = "planup_milestones";

    public static void init(Context context) {
        createChannels(context);
    }

    public static void show(Context context, String title, String message) {
        showNotification(context, title, message);
    }

    // 🔹 STANDARD NOTIFICATION
    public static void showNotification(Context context, String title, String message) {
        createChannels(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    // 🔹 BIG TEXT STYLE (For Daily Recap)
    public static void showBigTextNotification(Context context, String title, String message, String bigText) {
        createChannels(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(2002, builder.build());
        }
    }

    // 🔹 MILESTONE NOTIFICATION (With custom sound/importance)
    public static void showMilestoneNotification(Context context, String title, String message) {
        createChannels(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_MILESTONES)
                .setSmallIcon(R.drawable.ic_priority) // Using priority icon for milestones
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_EVENT);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(3003, builder.build());
        }
    }

    private static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            // Channel 1: Reminders
            NotificationChannel reminders = new NotificationChannel(
                    CHANNEL_ID, "PlanUp Reminders", NotificationManager.IMPORTANCE_HIGH);
            
            // Channel 2: Milestones
            NotificationChannel milestones = new NotificationChannel(
                    CHANNEL_MILESTONES, "Milestones & Achievements", NotificationManager.IMPORTANCE_HIGH);
            milestones.setDescription("Celebrate your productivity streaks!");
            milestones.enableVibration(true);

            manager.createNotificationChannel(reminders);
            manager.createNotificationChannel(milestones);
        }
    }
}
