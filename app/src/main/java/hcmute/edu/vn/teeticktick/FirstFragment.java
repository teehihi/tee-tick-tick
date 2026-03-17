package hcmute.edu.vn.teeticktick;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.teeticktick.database.TaskEntity;
import hcmute.edu.vn.teeticktick.databinding.FragmentFirstBinding;
import hcmute.edu.vn.teeticktick.viewmodel.TaskViewModel;
import hcmute.edu.vn.teeticktick.service.TaskReminderScheduler;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private TaskGroupAdapter adapter;
    private List<Object> items = new ArrayList<>();
    private TaskGroup gettingStartedGroup;
    private TaskGroup keyFeaturesGroup;
    private TaskGroup exploreMoreGroup;
    private TaskViewModel taskViewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        
        setupInitialData();
        
        adapter = new TaskGroupAdapter(items);
        adapter.setOnTaskDeleteListener((task, position) -> {
            // Xóa task khỏi database nếu có ID
            if (task.getId() > 0) {
                TaskEntity taskEntity = new TaskEntity(
                    task.getTitle(),
                    "",
                    task.getEmoji(),
                    task.isCompleted(),
                    "",
                    0,
                    0,
                    null,
                    null
                );
                taskEntity.setId(task.getId());
                taskViewModel.delete(taskEntity);
                android.util.Log.d("DATABASE", "Deleted task from database: " + task.getTitle() + " (ID: " + task.getId() + ")");
            }
        });
        adapter.setOnTaskCheckedChangeListener((task, isChecked) -> {
            // Cập nhật trạng thái completed trong database
            if (task.getId() > 0) {
                taskViewModel.updateTaskStatus(task.getId(), isChecked);
                android.util.Log.d("DATABASE", "Updated task completed status: " + task.getTitle() + " = " + isChecked);
                
                // Cancel reminder when task is completed
                if (isChecked && getContext() != null) {
                    TaskReminderScheduler.cancelReminder(getContext(), task.getId());
                }
            }
        });
        adapter.setOnTaskClickListener(task -> {
            // Mở TaskDetailBottomSheet để xem và chỉnh sửa task
            if (task.getId() > 0) {
                showTaskDetailDialog(task);
            }
        });
        binding.recyclerviewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerviewTasks.setAdapter(adapter);
        
        // Restore previous filter instead of relying on default call
        String filter = taskViewModel.getCurrentFilter();
        
        // Re-apply the filter
        switch (filter) {
            case "Today": filterByToday(); break;
            case "Tomorrow": filterByTomorrow(); break;
            case "Next 7 Days": filterByNext7Days(); break;
            case "Inbox": filterByInbox(); break;
            case "All": filterByAll(); break;
            case "Assigned to Me": filterByAssignedToMe(); break;
            case "Completed": filterByCompleted(); break;
            case "Won't Do": filterByWontDo(); break;
            default: filterByList(filter); break;
        }
    }

    private void setupInitialData() {
        // Getting Started group
        gettingStartedGroup = new TaskGroup(getString(R.string.getting_started));
        items.add(gettingStartedGroup);

        // Key Features group
        keyFeaturesGroup = new TaskGroup(getString(R.string.key_features));
        keyFeaturesGroup.addTask(new Task("📅", getString(R.string.calendar_check_schedule)));
        keyFeaturesGroup.addTask(new Task("🎯", getString(R.string.eisenhower_matrix)));
        keyFeaturesGroup.addTask(new Task("🍅", getString(R.string.pomo_beat_procrastination)));
        keyFeaturesGroup.addTask(new Task("🌸", getString(R.string.habit_visualize_efforts)));
        keyFeaturesGroup.addTask(new Task("✨", getString(R.string.more_features)));
        items.add(keyFeaturesGroup);
        items.addAll(keyFeaturesGroup.getTasks());

        // Explore More group
        exploreMoreGroup = new TaskGroup(getString(R.string.explore_more));
        exploreMoreGroup.addTask(new Task("💎", getString(R.string.premium)));
        exploreMoreGroup.addTask(new Task("💡", getString(R.string.follow_us)));
        items.add(exploreMoreGroup);
        items.addAll(exploreMoreGroup.getTasks());
    }

    // observeTasks() removed because we now rely on individual filter methods.

    public void addTask(String title, String description, String emoji, String listName, Long startDate, Long dueDate) {
        String targetList = listName;
        if (targetList == null || targetList.isEmpty()) {
            targetList = taskViewModel.getCurrentFilter();
            if (targetList.equals("Today") || targetList.equals("Tomorrow") ||
                targetList.equals("Next 7 Days") || targetList.equals("All") ||
                targetList.equals("Assigned to Me") || targetList.equals("Completed") ||
                targetList.equals("Won't Do") || targetList.equals("Welcome")) {
                targetList = "Inbox";
            }
        }

        android.util.Log.d("DATABASE", "Adding task: " + title + " | Emoji: " + emoji + " | List: " + targetList);
        TaskEntity taskEntity = new TaskEntity(
            title, description, emoji, false, targetList, 0,
            System.currentTimeMillis(), startDate, dueDate
        );

        final Long finalDueDate = dueDate;
        final String finalEmoji = emoji;
        taskViewModel.insertAndGetId(taskEntity, taskId -> {
            if (finalDueDate != null && finalDueDate > 0 && getContext() != null) {
                TaskReminderScheduler.scheduleReminder(
                    getContext(), taskId, title, finalEmoji, finalDueDate
                );
            }
        });
    }

    public void filterByToday() {
        taskViewModel.setCurrentFilter("Today");
        hideWelcomeGroups();
        updateTitle(getString(R.string.list_today));
        long startOfDay = getStartOfDay();
        long endOfDay = getEndOfDay();
        LiveData<List<TaskEntity>> liveData = taskViewModel.getTasksByDateRange(startOfDay, endOfDay);
        liveData.removeObservers(getViewLifecycleOwner());
        liveData.observe(getViewLifecycleOwner(), this::updateTasksFromDatabase);
    }

    public void filterByTomorrow() {
        taskViewModel.setCurrentFilter("Tomorrow");
        hideWelcomeGroups();
        updateTitle(getString(R.string.smart_list_tomorrow));
        long startOfTomorrow = getStartOfDay() + (24 * 60 * 60 * 1000L);
        long endOfTomorrow = getEndOfDay() + (24 * 60 * 60 * 1000L);
        LiveData<List<TaskEntity>> liveData = taskViewModel.getTasksByDateRange(startOfTomorrow, endOfTomorrow);
        liveData.removeObservers(getViewLifecycleOwner());
        liveData.observe(getViewLifecycleOwner(), this::updateTasksFromDatabase);
    }

    public void filterByNext7Days() {
        taskViewModel.setCurrentFilter("Next 7 Days");
        hideWelcomeGroups();
        updateTitle(getString(R.string.smart_list_next_7_days));
        long startOfDay = getStartOfDay();
        long endOf7Days = startOfDay + (7 * 24 * 60 * 60 * 1000L);
        LiveData<List<TaskEntity>> liveData = taskViewModel.getTasksByDateRange(startOfDay, endOf7Days);
        liveData.removeObservers(getViewLifecycleOwner());
        liveData.observe(getViewLifecycleOwner(), this::updateTasksFromDatabase);
    }

    public void filterByInbox() {
        taskViewModel.setCurrentFilter("Inbox");
        hideWelcomeGroups();
        updateTitle(getString(R.string.list_inbox));
        LiveData<List<TaskEntity>> liveData = taskViewModel.getIncompleteTasks();
        liveData.removeObservers(getViewLifecycleOwner());
        liveData.observe(getViewLifecycleOwner(), this::updateTasksFromDatabase);
    }

    public void filterByAll() {
        taskViewModel.setCurrentFilter("All");
        hideWelcomeGroups();
        updateTitle(getString(R.string.smart_list_all));
        LiveData<List<TaskEntity>> liveData = taskViewModel.getAllTasks();
        liveData.removeObservers(getViewLifecycleOwner());
        liveData.observe(getViewLifecycleOwner(), this::updateTasksFromDatabase);
    }

    public void filterByList(String listName) {
        taskViewModel.setCurrentFilter(listName);
        if (listName.equals("Welcome")) {
            showWelcomeGroups();
            updateTitle("👋 " + getString(R.string.welcome));
        } else {
            hideWelcomeGroups();
            // Display localized name
            String displayName = getLocalizedListName(listName);
            updateTitle(displayName);
        }
        LiveData<List<TaskEntity>> liveData = taskViewModel.getTasksByList(listName);
        liveData.removeObservers(getViewLifecycleOwner());
        liveData.observe(getViewLifecycleOwner(), this::updateTasksFromDatabase);
    }
    
    private String getLocalizedListName(String key) {
        switch (key) {
            case "Work":
                return getString(R.string.list_work);
            case "Personal":
                return getString(R.string.list_personal);
            case "Shopping":
                return getString(R.string.list_shopping);
            case "Learning":
                return getString(R.string.list_learning);
            case "Inbox":
                return getString(R.string.list_inbox);
            case "Today":
                return getString(R.string.list_today);
            default:
                return key;
        }
    }
    
    public void filterByAssignedToMe() {
        taskViewModel.setCurrentFilter("Assigned to Me");
        hideWelcomeGroups();
        updateTitle(getString(R.string.smart_list_assigned_to_me));
        LiveData<List<TaskEntity>> liveData = taskViewModel.getTasksByList("Assigned to Me");
        liveData.removeObservers(getViewLifecycleOwner());
        liveData.observe(getViewLifecycleOwner(), this::updateTasksFromDatabase);
    }
    
    public void filterByCompleted() {
        taskViewModel.setCurrentFilter("Completed");
        hideWelcomeGroups();
        updateTitle(getString(R.string.smart_list_completed));
        LiveData<List<TaskEntity>> liveData = taskViewModel.getCompletedTasks();
        liveData.removeObservers(getViewLifecycleOwner());
        liveData.observe(getViewLifecycleOwner(), this::updateTasksFromDatabase);
    }
    
    public void filterByWontDo() {
        taskViewModel.setCurrentFilter("Won't Do");
        hideWelcomeGroups();
        updateTitle(getString(R.string.smart_list_wont_do));
        LiveData<List<TaskEntity>> liveData = taskViewModel.getTasksByList("Won't Do");
        liveData.removeObservers(getViewLifecycleOwner());
        liveData.observe(getViewLifecycleOwner(), this::updateTasksFromDatabase);
    }
    
    private void updateTitle(String title) {
        if (binding != null && binding.titleText != null) {
            binding.titleText.setText(title);
        }
    }

    private void updateTasksFromDatabase(List<TaskEntity> taskEntities) {
        if (taskEntities != null) {
            android.util.Log.d("DATABASE", "Tasks loaded: " + taskEntities.size());
            
            // Clear old tasks from Getting Started group
            gettingStartedGroup.getTasks().clear();
            
            // Add new tasks from database
            for (TaskEntity entity : taskEntities) {
                android.util.Log.d("DATABASE", "Task: " + entity.getTitle() + " | List: " + entity.getListName());
                // Tạo Task với ID từ database để có thể xóa sau này
                Task task = new Task(entity.getId(), entity.getEmoji(), entity.getTitle(), entity.isCompleted());
                gettingStartedGroup.addTask(task);
            }
            
            // Check if we're in Welcome mode or filter mode
            if (items.contains(gettingStartedGroup)) {
                // Welcome mode - show tasks under Getting Started group
                int groupIndex = items.indexOf(gettingStartedGroup);
                if (groupIndex != -1) {
                    // Remove old task items from list
                    while (groupIndex + 1 < items.size() && items.get(groupIndex + 1) instanceof Task) {
                        items.remove(groupIndex + 1);
                    }
                    
                    // Insert new tasks
                    items.addAll(groupIndex + 1, gettingStartedGroup.getTasks());
                }
            } else {
                // Filter mode - show tasks directly without group
                // Clear all existing items
                items.clear();
                
                // Add tasks directly
                items.addAll(gettingStartedGroup.getTasks());
            }
            
            adapter.notifyDataSetChanged();
        }
    }
    
    private void hideWelcomeGroups() {
        // Remove Getting Started group (but keep it in memory)
        int gettingStartedIndex = items.indexOf(gettingStartedGroup);
        if (gettingStartedIndex != -1) {
            items.remove(gettingStartedIndex); // Remove group header
            // Remove tasks
            for (int i = 0; i < gettingStartedGroup.getTasks().size(); i++) {
                items.remove(gettingStartedIndex);
            }
        }
        
        // Remove Key Features group and tasks
        int keyFeaturesIndex = items.indexOf(keyFeaturesGroup);
        if (keyFeaturesIndex != -1) {
            items.remove(keyFeaturesIndex); // Remove group header
            for (int i = 0; i < keyFeaturesGroup.getTasks().size(); i++) {
                items.remove(keyFeaturesIndex); // Remove tasks
            }
        }
        
        // Remove Explore More group and tasks
        int exploreMoreIndex = items.indexOf(exploreMoreGroup);
        if (exploreMoreIndex != -1) {
            items.remove(exploreMoreIndex); // Remove group header
            for (int i = 0; i < exploreMoreGroup.getTasks().size(); i++) {
                items.remove(exploreMoreIndex); // Remove tasks
            }
        }
        
        adapter.notifyDataSetChanged();
    }
    
    private void showWelcomeGroups() {
        // Add Getting Started group back if not present
        if (!items.contains(gettingStartedGroup)) {
            items.add(0, gettingStartedGroup);
            items.addAll(1, gettingStartedGroup.getTasks());
        }
        
        // Add Key Features group back if not present
        if (!items.contains(keyFeaturesGroup)) {
            int gettingStartedIndex = items.indexOf(gettingStartedGroup);
            int insertIndex = gettingStartedIndex + 1 + gettingStartedGroup.getTasks().size();
            
            items.add(insertIndex, keyFeaturesGroup);
            items.addAll(insertIndex + 1, keyFeaturesGroup.getTasks());
        }
        
        // Add Explore More group back if not present
        if (!items.contains(exploreMoreGroup)) {
            items.add(exploreMoreGroup);
            items.addAll(exploreMoreGroup.getTasks());
        }
        
        adapter.notifyDataSetChanged();
    }

    private long getStartOfDay() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getEndOfDay() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
        calendar.set(java.util.Calendar.MINUTE, 59);
        calendar.set(java.util.Calendar.SECOND, 59);
        calendar.set(java.util.Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Restore title based on current filter
        String currentFilter = taskViewModel.getCurrentFilter();
        if (currentFilter.equals("Welcome")) {
            updateTitle("👋 " + getString(R.string.welcome));
        } else {
            String displayName = getLocalizedListName(currentFilter);
            updateTitle(displayName);
        }
    }
    
    private void showTaskDetailDialog(Task task) {
        // Navigate to TaskDetailFragment
        Bundle bundle = new Bundle();
        bundle.putInt("taskId", task.getId());
        NavHostFragment.findNavController(FirstFragment.this)
            .navigate(R.id.TaskDetailFragment, bundle);
    }
}
