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
    public static final String ACTION_DONE = "com.example.planup.ACTION_DONE";

    public static void init(Context context) {
        createChannels(context);
    }

    public static void show(Context context, String title, String message) {
        showNotification(context, title, message);
    }

    // 🔹 STANDARD NOTIFICATION WITH CLICK ACTION
    public static void showNotification(Context context, String title, String message) {
        createChannels(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    // 🔹 TASK NOTIFICATION WITH "MARK AS DONE" ACTION
    public static void showTaskNotification(Context context, String title, String message, String taskId) {
        createChannels(context);

        // Intent to open app
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)System.currentTimeMillis(), 
                intent, PendingIntent.FLAG_IMMUTABLE);

        // Action Intent for "Done"
        Intent doneIntent = new Intent(context, TaskActionReceiver.class);
        doneIntent.setAction(ACTION_DONE);
        doneIntent.putExtra("task_id", taskId);
        PendingIntent donePendingIntent = PendingIntent.getBroadcast(context, (int)System.currentTimeMillis(), 
                doneIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_priority, "Mark as Done", donePendingIntent) // Action button
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(taskId.hashCode(), builder.build());
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

    // 🔹 MILESTONE NOTIFICATION
    public static void showMilestoneNotification(Context context, String title, String message) {
        createChannels(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_MILESTONES)
                .setSmallIcon(R.drawable.ic_priority)
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

            NotificationChannel reminders = new NotificationChannel(
                    CHANNEL_ID, "PlanUp Reminders", NotificationManager.IMPORTANCE_HIGH);
            reminders.setDescription("Time-sensitive task reminders");
            reminders.enableLights(true);
            reminders.setVibrationPattern(new long[]{0, 250, 250, 250});

            NotificationChannel milestones = new NotificationChannel(
                    CHANNEL_MILESTONES, "Milestones & Achievements", NotificationManager.IMPORTANCE_HIGH);
            milestones.setDescription("Celebrate your productivity streaks!");
            milestones.enableVibration(true);

            manager.createNotificationChannel(reminders);
            manager.createNotificationChannel(milestones);
        }
    }
}