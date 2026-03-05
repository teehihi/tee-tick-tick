package hcmute.edu.vn.teeticktick;

import java.util.ArrayList;
import java.util.List;

public class TaskGroup {
    private String title;
    private List<Task> tasks;
    private boolean isExpanded;
    private int taskCount;

    public TaskGroup(String title) {
        this.title = title;
        this.tasks = new ArrayList<>();
        this.isExpanded = true;
        this.taskCount = 0;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void addTask(Task task) {
        tasks.add(task);
        taskCount = tasks.size();
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public int getTaskCount() {
        return taskCount;
    }
}
