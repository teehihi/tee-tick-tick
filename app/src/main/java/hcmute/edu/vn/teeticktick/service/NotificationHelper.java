package hcmute.edu.vn.teeticktick.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.teeticktick.MainActivity;
import hcmute.edu.vn.teeticktick.R;

public class NotificationHelper {

    public static final String CHANNEL_REMINDER = "task_reminder_channel";
    public static final String CHANNEL_DAILY = "daily_summary_channel";
    
    // Category-specific channels (v2 — forces recreation with correct sounds)
    public static final String CHANNEL_INBOX    = "reminder_inbox_v2";
    public static final String CHANNEL_WORK     = "reminder_work_v2";
    public static final String CHANNEL_PERSONAL = "reminder_personal_v2";
    public static final String CHANNEL_SHOPPING = "reminder_shopping_v2";
    public static final String CHANNEL_LEARNING = "reminder_learning_v2";

    // Overdue reminder channel
    public static final String CHANNEL_OVERDUE  = "reminder_overdue_v1";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            // Default reminder channel (for backward compatibility)
            NotificationChannel reminderChannel = new NotificationChannel(
                    CHANNEL_REMINDER,
                    "Nhắc nhở deadline",
                    NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("Thông báo nhắc nhở khi task sắp đến hạn");
            reminderChannel.enableVibration(true);
            manager.createNotificationChannel(reminderChannel);
            
            // Create category-specific channels with custom sounds
            createCategoryChannel(context, manager, CHANNEL_INBOX,    "Nhắc nhở - Inbox",    "default");
            createCategoryChannel(context, manager, CHANNEL_WORK,     "Nhắc nhở - Công việc","work");
            createCategoryChannel(context, manager, CHANNEL_PERSONAL, "Nhắc nhở - Cá nhân",  "personal");
            createCategoryChannel(context, manager, CHANNEL_SHOPPING, "Nhắc nhở - Mua sắm",  "shopping");
            createCategoryChannel(context, manager, CHANNEL_LEARNING, "Nhắc nhở - Học tập",  "learning");

            // Overdue channel with mixi_reminder sound
            createCategoryChannel(context, manager, CHANNEL_OVERDUE, "Task quá hạn", "overdue");

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
    
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void createCategoryChannel(Context context, NotificationManager manager, 
                                             String channelId, String channelName, String soundId) {
        try {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo cho " + channelName);
            channel.enableVibration(true);
            
            // Set custom sound
            Uri soundUri = hcmute.edu.vn.teeticktick.utils.NotificationSoundHelper.getSoundUri(context, soundId);
            android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttributes);
            
            manager.createNotificationChannel(channel);
        } catch (Exception e) {
            android.util.Log.e("NotificationHelper", "Error creating channel: " + channelId, e);
            // Create channel without custom sound as fallback
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo cho " + channelName);
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }
    }
    
    public static String getChannelIdForCategory(String categoryName) {
        if (categoryName == null) {
            return CHANNEL_REMINDER;
        }
        
        switch (categoryName.toLowerCase()) {
            case "inbox":
                return CHANNEL_INBOX;
            case "work":
                return CHANNEL_WORK;
            case "personal":
                return CHANNEL_PERSONAL;
            case "shopping":
                return CHANNEL_SHOPPING;
            case "learning":
                return CHANNEL_LEARNING;
            default:
                return CHANNEL_REMINDER;
        }
    }

    public static Notification createReminderNotification(Context context, String title, String emoji, 
                                                         String message, Uri soundUri, String categoryName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Get appropriate channel for category
        String channelId = getChannelIdForCategory(categoryName);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS);
        
        // Note: Sound is set in channel, not here
        
        return builder.build();
    }

    public static Notification createOverdueNotification(Context context, String title, String emoji, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Builder(context, CHANNEL_OVERDUE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS)
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
