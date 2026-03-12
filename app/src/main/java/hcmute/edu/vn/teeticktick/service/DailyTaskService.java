package hcmute.edu.vn.teeticktick.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

import hcmute.edu.vn.teeticktick.database.AppDatabase;
import hcmute.edu.vn.teeticktick.database.TaskEntity;

public class DailyTaskService extends Service {

    private static final String TAG = "DailyTaskService";
    private static final int NOTIFICATION_ID = 9999;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "DailyTaskService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "DailyTaskService started");

        // Start with a placeholder notification immediately to avoid ANR
        Notification placeholder = NotificationHelper.createDailySummaryNotification(this, 0);
        startForeground(NOTIFICATION_ID, placeholder);

        // Query database on background thread and update notification
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(getApplicationContext());

                long startOfDay = getStartOfDay();
                long endOfDay = getEndOfDay();

                List<TaskEntity> todayTasks = database.taskDao().getIncompleteTasksByDateRangeSync(startOfDay, endOfDay);
                int taskCount = (todayTasks != null) ? todayTasks.size() : 0;

                Log.d(TAG, "Today's incomplete tasks: " + taskCount);

                // Update the notification with actual count
                Notification updated = NotificationHelper.createDailySummaryNotification(this, taskCount);
                android.app.NotificationManager manager = 
                        (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.notify(NOTIFICATION_ID, updated);

            } catch (Exception e) {
                Log.e(TAG, "Error querying tasks", e);
            }
        });

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DailyTaskService destroyed");
    }

    private long getStartOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getEndOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }
}
