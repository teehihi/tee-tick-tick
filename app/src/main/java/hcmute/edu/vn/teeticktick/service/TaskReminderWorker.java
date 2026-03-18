package hcmute.edu.vn.teeticktick.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import hcmute.edu.vn.teeticktick.database.AppDatabase;
import hcmute.edu.vn.teeticktick.database.CategoryEntity;
import hcmute.edu.vn.teeticktick.database.TaskEntity;
import hcmute.edu.vn.teeticktick.utils.NotificationSoundHelper;

public class TaskReminderWorker extends Worker {

    private static final String TAG = "TaskReminderWorker";

    public static final String KEY_TASK_ID = "task_id";
    public static final String KEY_TASK_TITLE = "task_title";
    public static final String KEY_TASK_EMOJI = "task_emoji";

    public TaskReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        int taskId = getInputData().getInt(KEY_TASK_ID, -1);
        String taskTitle = getInputData().getString(KEY_TASK_TITLE);
        String taskEmoji = getInputData().getString(KEY_TASK_EMOJI);

        if (taskId == -1 || taskTitle == null) {
            Log.e(TAG, "Invalid input data");
            return Result.failure();
        }

        // Check if task still exists and is not completed
        AppDatabase database = AppDatabase.getInstance(getApplicationContext());
        TaskEntity task = database.taskDao().getTaskByIdSync(taskId);

        if (task == null) {
            Log.d(TAG, "Task no longer exists, skipping reminder");
            return Result.success();
        }

        if (task.isCompleted()) {
            Log.d(TAG, "Task already completed, skipping reminder");
            return Result.success();
        }

        // Check notification permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted");
                return Result.failure();
            }
        }

        // Send reminder notification
        String emoji = (taskEmoji != null && !taskEmoji.isEmpty()) ? taskEmoji : "⏰";
        
        // Get category name for channel selection
        String categoryName = task.getListName();
        Log.d(TAG, "Task listName: " + categoryName);
        
        // Get notification sound from category (for logging)
        Uri soundUri = null;
        if (categoryName != null) {
            CategoryEntity category = database.categoryDao().getCategoryByNameSync(categoryName);
            if (category != null) {
                String soundId = category.getNotificationSound();
                Log.d(TAG, "Category found: " + category.getName() + ", soundId: " + soundId);
                
                if (soundId != null) {
                    soundUri = NotificationSoundHelper.getSoundUri(
                        getApplicationContext(), 
                        soundId
                    );
                    Log.d(TAG, "Sound URI: " + soundUri);
                } else {
                    Log.w(TAG, "Category has no notification sound set");
                }
            } else {
                Log.w(TAG, "Category not found for listName: " + categoryName);
            }
        } else {
            Log.w(TAG, "Task has no listName");
        }
        
        Notification notification = NotificationHelper.createReminderNotification(
                getApplicationContext(),
                taskTitle,
                emoji,
                "⏰ Task sắp đến hạn! Hãy hoàn thành ngay.",
                soundUri,
                categoryName
        );

        NotificationManager manager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(taskId, notification);

        Log.d(TAG, "Reminder sent for task: " + taskTitle);
        return Result.success();
    }
}
