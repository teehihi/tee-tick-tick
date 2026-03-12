package hcmute.edu.vn.teeticktick.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.teeticktick.MainActivity;
import hcmute.edu.vn.teeticktick.R;

public class NotificationHelper {

    public static final String CHANNEL_REMINDER = "task_reminder_channel";
    public static final String CHANNEL_DAILY = "daily_summary_channel";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            // Channel for task deadline reminders
            NotificationChannel reminderChannel = new NotificationChannel(
                    CHANNEL_REMINDER,
                    "Nhắc nhở deadline",
                    NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("Thông báo nhắc nhở khi task sắp đến hạn");
            reminderChannel.enableVibration(true);
            manager.createNotificationChannel(reminderChannel);

            // Channel for daily summary (foreground service)
            NotificationChannel dailyChannel = new NotificationChannel(
                    CHANNEL_DAILY,
                    "Tổng quan hàng ngày",
                    NotificationManager.IMPORTANCE_LOW
            );
            dailyChannel.setDescription("Hiển thị tổng số task hôm nay");
            manager.createNotificationChannel(dailyChannel);
        }
    }

    public static Notification createReminderNotification(Context context, String title, String emoji, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, CHANNEL_REMINDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(emoji + " " + title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build();
    }

    public static Notification createDailySummaryNotification(Context context, int taskCount) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String contentText;
        if (taskCount == 0) {
            contentText = "🎉 Bạn đã hoàn thành tất cả task hôm nay!";
        } else {
            contentText = "Bạn có " + taskCount + " task cần hoàn thành hôm nay";
        }

        return new NotificationCompat.Builder(context, CHANNEL_DAILY)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("📋 TeeTickTick")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}
