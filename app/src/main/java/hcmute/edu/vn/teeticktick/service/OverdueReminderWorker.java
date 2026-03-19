package hcmute.edu.vn.teeticktick.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import hcmute.edu.vn.teeticktick.database.AppDatabase;
import hcmute.edu.vn.teeticktick.database.TaskEntity;

public class OverdueReminderWorker extends Worker {

    private static final String TAG = "OverdueReminderWorker";

    public static final String KEY_TASK_ID    = "task_id";
    public static final String KEY_TASK_TITLE = "task_title";
    public static final String KEY_TASK_EMOJI = "task_emoji";
    // Which overdue ping this is: 0=deadline, 1=+1h, 2=+2h, 3=+1day
    public static final String KEY_PING_INDEX = "ping_index";

    private static final String[] MESSAGES = {
        "⏰ Task đã đến hạn! Hãy hoàn thành ngay.",
        "🔔 Task vẫn chưa xong sau 1 giờ. Đừng bỏ quên nhé!",
        "⚠️ Task quá hạn 2 giờ rồi. Xử lý ngay thôi!",
        "🚨 Task quá hạn 1 ngày! Hãy hoàn thành hoặc dời deadline."
    };

    public OverdueReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int taskId    = getInputData().getInt(KEY_TASK_ID, -1);
        String title  = getInputData().getString(KEY_TASK_TITLE);
        String emoji  = getInputData().getString(KEY_TASK_EMOJI);
        int pingIndex = getInputData().getInt(KEY_PING_INDEX, 0);

        if (taskId == -1 || title == null) return Result.failure();

        // Check task still exists and is NOT completed
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        TaskEntity task = db.taskDao().getTaskByIdSync(taskId);
        if (task == null || task.isCompleted()) {
            Log.d(TAG, "Task done or deleted, skipping overdue ping " + pingIndex);
            return Result.success();
        }

        // Check notification permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return Result.failure();
            }
        }

        String emojiStr = (emoji != null && !emoji.isEmpty()) ? emoji : "⏰";
        String message  = pingIndex < MESSAGES.length ? MESSAGES[pingIndex] : MESSAGES[MESSAGES.length - 1];

        Notification notification = NotificationHelper.createOverdueNotification(
                getApplicationContext(), title, emojiStr, message);

        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        // Use unique ID per ping so they stack instead of replacing each other
        manager.notify(taskId * 10 + pingIndex, notification);

        Log.d(TAG, "Overdue ping " + pingIndex + " sent for: " + title);
        return Result.success();
    }
}
