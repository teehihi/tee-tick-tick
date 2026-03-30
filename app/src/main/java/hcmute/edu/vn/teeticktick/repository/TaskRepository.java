package hcmute.edu.vn.teeticktick.repository;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.teeticktick.database.AppDatabase;
import hcmute.edu.vn.teeticktick.database.TaskDao;
import hcmute.edu.vn.teeticktick.database.TaskEntity;

public class TaskRepository {

    private static final String TAG = "TaskRepository";

    private TaskDao taskDao;
    private CalendarRepository calendarRepository;
    private ExecutorService executorService;

    public TaskRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        taskDao = database.taskDao();
        calendarRepository = new CalendarRepository(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    // Constructor nhận Context (dùng khi không có Application)
    public TaskRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        taskDao = database.taskDao();
        calendarRepository = new CalendarRepository(context);
        executorService = Executors.newSingleThreadExecutor();
    }

    // -----------------------------------------------------------------------
    // Insert
    // -----------------------------------------------------------------------

    public void insert(TaskEntity task) {
        executorService.execute(() -> {
            long roomId = taskDao.insert(task);
            task.setId((int) roomId);
            // Sync sang Calendar
            long eventId = calendarRepository.addEventForTask(task);
            if (eventId != -1) {
                taskDao.updateCalendarEventId((int) roomId, eventId);
                Log.d(TAG, "Inserted task & synced to Calendar: eventId=" + eventId);
            }
        });
    }

    public void insertAndGetId(TaskEntity task, OnTaskInsertedListener listener) {
        executorService.execute(() -> {
            long roomId = taskDao.insert(task);
            task.setId((int) roomId);
            // Sync sang Calendar
            long eventId = calendarRepository.addEventForTask(task);
            if (eventId != -1) {
                taskDao.updateCalendarEventId((int) roomId, eventId);
            }
            if (listener != null) {
                listener.onTaskInserted((int) roomId);
            }
        });
    }

    public interface OnTaskInsertedListener {
        void onTaskInserted(int taskId);
    }

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------

    public void update(TaskEntity task) {
        executorService.execute(() -> {
            taskDao.update(task);
            // Sync cập nhật sang Calendar
            calendarRepository.updateEventForTask(task);
            Log.d(TAG, "Updated task & synced to Calendar: taskId=" + task.getId());
        });
    }

    public void updateTaskStatus(int taskId, boolean isCompleted) {
        executorService.execute(() -> {
            taskDao.updateTaskStatus(taskId, isCompleted);
            // Lấy task mới nhất để sync Calendar (cập nhật icon ✅/⬜)
            TaskEntity task = taskDao.getTaskByIdSync(taskId);
            if (task != null) {
                calendarRepository.updateEventForTask(task);
            }
        });
    }

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------

    public void delete(TaskEntity task) {
        executorService.execute(() -> {
            // Xóa event Calendar trước khi xóa khỏi Room
            calendarRepository.deleteEventForTask(task);
            taskDao.delete(task);
            Log.d(TAG, "Deleted task & removed from Calendar: taskId=" + task.getId());
        });
    }

    // -----------------------------------------------------------------------
    // Queries (LiveData — không thay đổi)
    // -----------------------------------------------------------------------

    public LiveData<List<TaskEntity>> getTasksByList(String listName) {
        return taskDao.getTasksByList(listName);
    }

    public LiveData<List<TaskEntity>> getAllTasks() {
        return taskDao.getAllTasks();
    }

    public LiveData<List<TaskEntity>> getIncompleteTasks() {
        return taskDao.getIncompleteTasks();
    }

    public LiveData<List<TaskEntity>> getCompletedTasks() {
        return taskDao.getCompletedTasks();
    }

    public LiveData<List<TaskEntity>> getTasksByDateRange(long startDate, long endDate) {
        return taskDao.getTasksByDateRange(startDate, endDate);
    }

    public void deleteCompletedTasks() {
        executorService.execute(() -> {
            // Lấy toàn bộ completed tasks để xóa event Calendar kèm theo
            List<TaskEntity> completed = taskDao.getAllTasksSync();
            if (completed != null) {
                for (TaskEntity t : completed) {
                    if (t.isCompleted()) {
                        calendarRepository.deleteEventForTask(t);
                    }
                }
            }
            taskDao.deleteCompletedTasks();
        });
    }

    public LiveData<Integer> getActiveTaskCountByList(String listName) {
        return taskDao.getActiveTaskCountByList(listName);
    }

    public LiveData<Integer> getActiveIncompleteTaskCount() {
        return taskDao.getActiveIncompleteTaskCount();
    }

    public LiveData<Integer> getActiveTaskCountByDateRange(long startDate, long endDate) {
        return taskDao.getActiveTaskCountByDateRange(startDate, endDate);
    }
}
