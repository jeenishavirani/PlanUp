package com.example.planup;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.planup.utils.TaskActionReceiver;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra("taskTitle");
        String taskId = intent.getStringExtra("taskId");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Main intent to open the app
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Intent for "Mark as Done" action
        Intent doneIntent = new Intent(context, TaskActionReceiver.class);
        doneIntent.setAction("ACTION_DONE");
        doneIntent.putExtra("taskId", taskId);
        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                context, taskId.hashCode(), doneIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("task_alarms", "Task Alarms", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "task_alarms")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Task Reminder")
                .setContentText(taskTitle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                // 🔹 Add "Mark as Done" button
                .addAction(android.R.drawable.ic_menu_save, "Mark as Done", donePendingIntent);

        notificationManager.notify(taskId.hashCode(), builder.build());
    }
}
