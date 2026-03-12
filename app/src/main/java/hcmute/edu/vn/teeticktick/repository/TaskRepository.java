package hcmute.edu.vn.teeticktick.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.teeticktick.database.AppDatabase;
import hcmute.edu.vn.teeticktick.database.TaskDao;
import hcmute.edu.vn.teeticktick.database.TaskEntity;

public class TaskRepository {
    
    private TaskDao taskDao;
    private ExecutorService executorService;
    
    public TaskRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        taskDao = database.taskDao();
        executorService = Executors.newSingleThreadExecutor();
    }
    
    public void insert(TaskEntity task) {
        executorService.execute(() -> taskDao.insert(task));
    }
    
    public void insertAndGetId(TaskEntity task, OnTaskInsertedListener listener) {
        executorService.execute(() -> {
            long id = taskDao.insert(task);
            if (listener != null) {
                listener.onTaskInserted((int) id);
            }
        });
    }
    
    public interface OnTaskInsertedListener {
        void onTaskInserted(int taskId);
    }
    
    public void update(TaskEntity task) {
        executorService.execute(() -> taskDao.update(task));
    }
    
    public void delete(TaskEntity task) {
        executorService.execute(() -> taskDao.delete(task));
    }
    
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
        executorService.execute(() -> taskDao.deleteCompletedTasks());
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
