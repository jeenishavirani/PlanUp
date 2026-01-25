package com.example.planup;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class ReminderScheduler {

    private static final String TAG = "ReminderScheduler";

    public static void scheduleDailyMorningReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // setRepeating is not affected by the Exact Alarm restriction
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    public static void scheduleTwoHourBeforeReminder(Context context, long taskTimeMillis, int taskId, String taskTitle) {
        long reminderTime = taskTimeMillis - (2 * 60 * 60 * 1000);
        if (reminderTime <= System.currentTimeMillis()) return;
        scheduleReminder(context, reminderTime, taskId + 2000, "TWO_HOUR", taskTitle);
    }

    public static void scheduleFiveMinuteBeforeReminder(Context context, long taskTimeMillis, int taskId, String taskTitle) {
        long reminderTime = taskTimeMillis - (5 * 60 * 1000);
        if (reminderTime <= System.currentTimeMillis()) return;
        scheduleReminder(context, reminderTime, taskId + 3000, "FIVE_MIN", taskTitle);
    }

    public static void scheduleMissedTaskReminder(Context context, long taskTimeMillis, int taskId, String taskTitle, String taskIdStr) {
        long reminderTime = taskTimeMillis + (5 * 60 * 1000); 
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("type", "MISSED");
        intent.putExtra("taskId", taskId);
        intent.putExtra("taskIdStr", taskIdStr);
        intent.putExtra("taskTitle", taskTitle);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId + 4000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                } else {
                    // Fallback to inexact alarm if permission is missing
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: Cannot schedule exact alarm", e);
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        }
    }

    private static void scheduleReminder(Context context, long timeMillis, int requestCode, String type, String taskTitle) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("type", type);
        intent.putExtra("taskTitle", taskTitle);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
                } else {
                    // Fallback to inexact alarm if permission is missing
                    alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: Cannot schedule exact alarm", e);
            // Fallback for safety
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
        }
    }
}
