package com.example.planup;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.planup.utils.NotificationHelper;

import java.util.Random;
public class ReminderReceiver extends BroadcastReceiver{

    private final String[] morningMessages = {
            "🌞 Fresh start! A new day, a new chance 💜",
            "Good morning ☀️ One small task can change your day",
            "New day. New energy. Let’s go 🚀",
            "Start slow, stay consistent 🌱",
            "You’ve got this 💪 Let’s make today count"
    };

    @Override
    public void onReceive(Context context, Intent intent) {

        // Pick random friendly message
        String message = morningMessages[
                new Random().nextInt(morningMessages.length)
                ];

        // Show notification
        NotificationHelper.showNotification(
                context,
                "PlanUp 🌅",
                message
        );
    }

}
