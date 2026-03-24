package hcmute.edu.vn.teeticktick.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {
    
    @Insert
    long insert(TaskEntity task);
    
    @Update
    void update(TaskEntity task);
    
    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    void updateTaskStatus(int taskId, boolean isCompleted);
    
    @Delete
    void delete(TaskEntity task);
    
    @Query("SELECT * FROM tasks WHERE listName = :listName ORDER BY createdAt DESC")
    LiveData<List<TaskEntity>> getTasksByList(String listName);
    
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    LiveData<List<TaskEntity>> getAllTasks();
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    LiveData<List<TaskEntity>> getIncompleteTasks();
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY createdAt DESC")
    LiveData<List<TaskEntity>> getCompletedTasks();
    
    @Query("SELECT * FROM tasks WHERE dueDate IS NOT NULL AND dueDate >= :startDate AND dueDate <= :endDate ORDER BY dueDate ASC")
    LiveData<List<TaskEntity>> getTasksByDateRange(long startDate, long endDate);
    
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0 AND listName = :listName")
    LiveData<Integer> getActiveTaskCountByList(String listName);
    
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    LiveData<Integer> getActiveIncompleteTaskCount();
    
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0 AND dueDate >= :startDate AND dueDate <= :endDate")
    LiveData<Integer> getActiveTaskCountByDateRange(long startDate, long endDate);
    
    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    void deleteCompletedTasks();
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    TaskEntity getTaskByIdSync(int taskId);
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate >= :startDate AND dueDate <= :endDate ORDER BY dueDate ASC")
    List<TaskEntity> getIncompleteTasksByDateRangeSync(long startDate, long endDate);
    
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    List<TaskEntity> getAllTasksSync();
}
