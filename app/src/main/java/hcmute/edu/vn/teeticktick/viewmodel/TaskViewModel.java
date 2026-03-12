package hcmute.edu.vn.teeticktick.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import hcmute.edu.vn.teeticktick.database.TaskEntity;
import hcmute.edu.vn.teeticktick.repository.TaskRepository;

public class TaskViewModel extends AndroidViewModel {
    
    private TaskRepository repository;
    private LiveData<List<TaskEntity>> allTasks;
    
    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
        allTasks = repository.getAllTasks();
    }
    
    public void insert(TaskEntity task) {
        repository.insert(task);
    }
    
    public void insertAndGetId(TaskEntity task, TaskRepository.OnTaskInsertedListener listener) {
        repository.insertAndGetId(task, listener);
    }
    
    public void update(TaskEntity task) {
        repository.update(task);
    }
    
    public void delete(TaskEntity task) {
        repository.delete(task);
    }
    
    public LiveData<List<TaskEntity>> getAllTasks() {
        return allTasks;
    }
    
    public LiveData<List<TaskEntity>> getTasksByList(String listName) {
        return repository.getTasksByList(listName);
    }
    
    public LiveData<List<TaskEntity>> getIncompleteTasks() {
        return repository.getIncompleteTasks();
    }
    
    public LiveData<List<TaskEntity>> getCompletedTasks() {
        return repository.getCompletedTasks();
    }
    
    public LiveData<List<TaskEntity>> getTasksByDateRange(long startDate, long endDate) {
        return repository.getTasksByDateRange(startDate, endDate);
    }
    
    public void deleteCompletedTasks() {
        repository.deleteCompletedTasks();
    }

    public LiveData<Integer> getActiveTaskCountByList(String listName) {
        return repository.getActiveTaskCountByList(listName);
    }
    
    public LiveData<Integer> getActiveIncompleteTaskCount() {
        return repository.getActiveIncompleteTaskCount();
    }
    
    public LiveData<Integer> getActiveTaskCountByDateRange(long startDate, long endDate) {
        return repository.getActiveTaskCountByDateRange(startDate, endDate);
    }
    
    // Store current filter to persist across fragment recreations
    private String currentFilter = "Welcome";
    
    public String getCurrentFilter() {
        return currentFilter;
    }
    
    public void setCurrentFilter(String filter) {
        this.currentFilter = filter;
    }
}
