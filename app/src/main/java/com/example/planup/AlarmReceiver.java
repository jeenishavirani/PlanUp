package com.example.planup;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.planup.utils.TaskActionReceiver;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra("taskTitle");
        String taskId = intent.getStringExtra("taskId");
        
        Log.d("AlarmReceiver", "Alarm received for task: " + taskTitle + " (ID: " + taskId + ")");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Main intent to open the app
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, taskId != null ? taskId.hashCode() : 0, 
                notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Intent for "Mark as Done" action
        Intent doneIntent = new Intent(context, TaskActionReceiver.class);
        doneIntent.setAction("ACTION_DONE");
        doneIntent.putExtra("taskId", taskId);
        
        int requestCode = (taskId != null) ? taskId.hashCode() : (int) System.currentTimeMillis();
        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                context, requestCode, doneIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = "task_alarms";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Task Alarms", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.applogo)
                .setContentTitle("Task Starting Now!")
                .setContentText(taskTitle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .addAction(android.R.drawable.ic_menu_save, "Mark as Done", donePendingIntent);

        int notificationId = (taskId != null) ? taskId.hashCode() : (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }
}
