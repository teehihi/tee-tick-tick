package hcmute.edu.vn.teeticktick;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import hcmute.edu.vn.teeticktick.database.TaskEntity;
import hcmute.edu.vn.teeticktick.databinding.FragmentTimelineBinding;
import hcmute.edu.vn.teeticktick.viewmodel.TaskViewModel;

public class TimelineFragment extends Fragment {

    private FragmentTimelineBinding binding;
    private TaskViewModel taskViewModel;
    private CalendarTaskAdapter adapter;
    private List<TaskEntity> taskList = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentTimelineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        
        // Setup RecyclerView
        adapter = new CalendarTaskAdapter(taskList);
        binding.timelineRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.timelineRecyclerview.setAdapter(adapter);
        
        // Setup adapter listeners
        setupAdapterListeners();
        
        // Setup filter buttons
        setupFilterButtons();
        
        // Load all tasks by default
        loadTasks("all");
    }
    
    private void setupAdapterListeners() {
        adapter.setOnTaskClickListener(task -> {
            showTaskDetailDialog(task);
        });
        
        adapter.setOnTaskCheckedChangeListener((task, isChecked) -> {
            task.setCompleted(isChecked);
            taskViewModel.update(task);
        });
    }
    
    private void setupFilterButtons() {
        binding.filterAll.setOnClickListener(v -> {
            selectFilter(binding.filterAll, "all");
            loadTasks("all");
        });
        
        binding.filterToday.setOnClickListener(v -> {
            selectFilter(binding.filterToday, "today");
            loadTasks("today");
        });
        
        binding.filterWeek.setOnClickListener(v -> {
            selectFilter(binding.filterWeek, "week");
            loadTasks("week");
        });
        
        binding.filterCompleted.setOnClickListener(v -> {
            selectFilter(binding.filterCompleted, "completed");
            loadTasks("completed");
        });
    }
    
    private void selectFilter(TextView selectedButton, String filter) {
        currentFilter = filter;
        
        // Reset all buttons
        binding.filterAll.setSelected(false);
        binding.filterToday.setSelected(false);
        binding.filterWeek.setSelected(false);
        binding.filterCompleted.setSelected(false);
        
        binding.filterAll.setTextColor(getResources().getColor(R.color.text_secondary, null));
        binding.filterToday.setTextColor(getResources().getColor(R.color.text_secondary, null));
        binding.filterWeek.setTextColor(getResources().getColor(R.color.text_secondary, null));
        binding.filterCompleted.setTextColor(getResources().getColor(R.color.text_secondary, null));
        
        // Select current button
        selectedButton.setSelected(true);
        selectedButton.setTextColor(getResources().getColor(R.color.white, null));
    }
    
    private void loadTasks(String filter) {
        switch (filter) {
            case "all":
                binding.timelineSubtitle.setText("Tất cả nhiệm vụ theo thời gian");
                taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), this::updateTaskList);
                break;
                
            case "today":
                binding.timelineSubtitle.setText("Nhiệm vụ hôm nay");
                long startOfDay = getStartOfDay(System.currentTimeMillis());
                long endOfDay = getEndOfDay(System.currentTimeMillis());
                taskViewModel.getTasksByDateRange(startOfDay, endOfDay).observe(getViewLifecycleOwner(), this::updateTaskList);
                break;
                
            case "week":
                binding.timelineSubtitle.setText("Nhiệm vụ tuần này");
                long startOfWeek = getStartOfDay(System.currentTimeMillis());
                long endOfWeek = startOfWeek + (7 * 24 * 60 * 60 * 1000L);
                taskViewModel.getTasksByDateRange(startOfWeek, endOfWeek).observe(getViewLifecycleOwner(), this::updateTaskList);
                break;
                
            case "completed":
                binding.timelineSubtitle.setText("Nhiệm vụ đã hoàn thành");
                taskViewModel.getCompletedTasks().observe(getViewLifecycleOwner(), this::updateTaskList);
                break;
        }
    }
    
    private void updateTaskList(List<TaskEntity> tasks) {
        if (tasks != null && !tasks.isEmpty()) {
            taskList.clear();
            taskList.addAll(tasks);
            adapter.updateTasks(taskList);
            
            binding.timelineRecyclerview.setVisibility(View.VISIBLE);
            binding.timelineEmptyState.setVisibility(View.GONE);
        } else {
            taskList.clear();
            adapter.updateTasks(taskList);
            
            binding.timelineRecyclerview.setVisibility(View.GONE);
            binding.timelineEmptyState.setVisibility(View.VISIBLE);
        }
    }
    
    private long getStartOfDay(long dateMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getEndOfDay(long dateMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }
    
    private void showTaskDetailDialog(TaskEntity task) {
        // Navigate to TaskDetailFragment
        Bundle bundle = new Bundle();
        bundle.putInt("taskId", task.getId());
        NavHostFragment.findNavController(TimelineFragment.this)
            .navigate(R.id.TaskDetailFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
