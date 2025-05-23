package com.example.newlife;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.newlife.MainActivity;

import java.util.Random;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "MOTIVATION_CHANNEL";
    private static final int NOTIFICATION_ID = 1;

    // Array of 10 motivational messages (in Russian)
    private final String[] MOTIVATIONAL_MESSAGES = {
            "Каждый маленький шаг приближает тебя к большой цели! Продолжай двигаться вперёд!",
            "Ты сильнее, чем думаешь! Сегодня - идеальный день для новых достижений!",
            "Не сдавайся! Именно сегодня ты можешь сделать то, что не смог вчера!",
            "Успех - это сумма маленьких усилий, повторяемых изо дня в день!",
            "Ты уникален! Миру нужно именно то, что можешь сделать только ты!",
            "Сложности - это возможности стать лучше! Прими вызов!",
            "Верь в себя! Ты способен на большее, чем можешь представить!",
            "Сегодняшние усилия - завтрашние победы! Не останавливайся!",
            "Каждая проблема - это скрытая возможность. Найди её!",
            "Ты ближе к цели, чем был вчера! Продолжай идти!"
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        showRandomMotivationalNotification(context);
    }

    private void showRandomMotivationalNotification(Context context) {
        createNotificationChannel(context);

        // Get random message
        String randomMessage = getRandomMotivationalMessage();

        // Create intent to open app
        Intent appIntent = new Intent(context, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Build notification with random message
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_announcement_24) // Use your own icon
                .setContentTitle("Твоя ежедневная мотивация!")
                .setContentText(randomMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(randomMessage))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Add action button (optional)
        builder.addAction(
                R.drawable.baseline_cached_24,
                "Я сделаю это!",
                pendingIntent
        );

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e("Notification", "Failed to show notification", e);
        }
    }

    private String getRandomMotivationalMessage() {
        Random random = new Random();
        return MOTIVATIONAL_MESSAGES[random.nextInt(MOTIVATIONAL_MESSAGES.length)];
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Мотивационные уведомления";
            String description = "Ежедневные мотивирующие сообщения";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}