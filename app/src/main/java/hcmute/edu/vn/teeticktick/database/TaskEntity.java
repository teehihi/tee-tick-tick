package hcmute.edu.vn.teeticktick.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String title;
    private String description;
    private String emoji;
    private boolean isCompleted;
    private String listName;
    private int priority;
    private long createdAt;
    private Long startDate;  // user-selected start datetime
    private Long dueDate;    // user-selected end/deadline datetime
    private Long calendarEventId; // ID của event trong Android Calendar (null nếu chưa sync)

    public TaskEntity(String title, String description, String emoji, boolean isCompleted,
                      String listName, int priority, long createdAt, Long startDate, Long dueDate) {
        this.title = title;
        this.description = description;
        this.emoji = emoji;
        this.isCompleted = isCompleted;
        this.listName = listName;
        this.priority = priority;
        this.createdAt = createdAt;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.calendarEventId = null;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public Long getDueDate() {
        return dueDate;
    }

    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
    }

    public Long getCalendarEventId() {
        return calendarEventId;
    }

    public void setCalendarEventId(Long calendarEventId) {
        this.calendarEventId = calendarEventId;
    }
}
