package hcmute.edu.vn.teeticktick.service;

import android.content.Context;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class TaskReminderScheduler {

    private static final String TAG = "TaskReminderScheduler";
    private static final long REMINDER_BEFORE_MS = 30 * 60 * 1000L; // 30 minutes before deadline

    /**
     * Schedule a reminder notification for a task.
     * The reminder fires 30 minutes before the dueDate.
     * If the dueDate is less than 30 minutes away, the reminder fires immediately.
     */
    public static void scheduleReminder(Context context, int taskId, String title, String emoji, long dueDate) {
        long now = System.currentTimeMillis();
        long reminderTime = dueDate - REMINDER_BEFORE_MS;
        long delay = Math.max(0, reminderTime - now);

        if (dueDate <= now) {
            Log.d(TAG, "Task dueDate already passed, skipping reminder for: " + title);
            return;
        }

        Data inputData = new Data.Builder()
                .putInt(TaskReminderWorker.KEY_TASK_ID, taskId)
                .putString(TaskReminderWorker.KEY_TASK_TITLE, title)
                .putString(TaskReminderWorker.KEY_TASK_EMOJI, emoji)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TaskReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();

        String uniqueWorkName = "task_reminder_" + taskId;

        WorkManager.getInstance(context)
                .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest);

        long delayMinutes = delay / (60 * 1000);
        Log.d(TAG, "Scheduled reminder for task '" + title + "' in " + delayMinutes + " minutes");
    }

    /**
     * Cancel a scheduled reminder for a task.
     * Call when a task is completed or deleted.
     */
    public static void cancelReminder(Context context, int taskId) {
        String uniqueWorkName = "task_reminder_" + taskId;
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName);
        Log.d(TAG, "Cancelled reminder for task ID: " + taskId);
    }

    /**
     * Schedule overdue reminders: fires at deadline, +1h, +2h, +1day.
     * Each ping checks if task is still incomplete before notifying.
     */
    public static void scheduleOverdueReminders(Context context, int taskId, String title,
                                                String emoji, long dueDate) {
        long now = System.currentTimeMillis();
        if (dueDate <= now) return; // already past, don't schedule

        long[] offsets = {
            0L,                          // exactly at deadline
            60 * 60 * 1000L,             // +1 hour
            2 * 60 * 60 * 1000L,         // +2 hours
            24 * 60 * 60 * 1000L         // +1 day
        };

        WorkManager wm = WorkManager.getInstance(context);
        for (int i = 0; i < offsets.length; i++) {
            long fireAt = dueDate + offsets[i];
            long delay  = fireAt - now;
            if (delay < 0) continue; // already passed, skip

            Data inputData = new Data.Builder()
                    .putInt(OverdueReminderWorker.KEY_TASK_ID, taskId)
                    .putString(OverdueReminderWorker.KEY_TASK_TITLE, title)
                    .putString(OverdueReminderWorker.KEY_TASK_EMOJI, emoji)
                    .putInt(OverdueReminderWorker.KEY_PING_INDEX, i)
                    .build();

            OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(OverdueReminderWorker.class)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .build();

            wm.enqueueUniqueWork(
                    "overdue_" + taskId + "_" + i,
                    ExistingWorkPolicy.REPLACE,
                    req
            );
        }
        Log.d(TAG, "Scheduled overdue reminders for task: " + title);
    }

    /**
     * Cancel all overdue reminders for a task (on complete/delete/reschedule).
     */
    public static void cancelOverdueReminders(Context context, int taskId) {
        WorkManager wm = WorkManager.getInstance(context);
        for (int i = 0; i < 4; i++) {
            wm.cancelUniqueWork("overdue_" + taskId + "_" + i);
        }
        Log.d(TAG, "Cancelled overdue reminders for task ID: " + taskId);
    }
}
