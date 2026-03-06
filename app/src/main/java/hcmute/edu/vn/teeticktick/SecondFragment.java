package hcmute.edu.vn.teeticktick;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.teeticktick.database.TaskEntity;
import hcmute.edu.vn.teeticktick.databinding.FragmentSecondBinding;
import hcmute.edu.vn.teeticktick.viewmodel.TaskViewModel;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private TaskViewModel taskViewModel;
    private CalendarTaskAdapter adapter;
    private List<TaskEntity> taskList = new ArrayList<>();
    private long selectedDate;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        
        // Setup RecyclerView with new adapter
        adapter = new CalendarTaskAdapter(taskList);
        binding.calendarTasksRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.calendarTasksRecyclerview.setAdapter(adapter);
        
        // Setup adapter listeners
        setupAdapterListeners();
        
        // Set default selected date to today
        selectedDate = System.currentTimeMillis();
        updateMonthYearText(selectedDate);
        updateSelectedDateText(selectedDate);
        loadTasksForDate(selectedDate);
        
        // Calendar date change listener
        binding.calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                selectedDate = calendar.getTimeInMillis();
                
                updateSelectedDateText(selectedDate);
                loadTasksForDate(selectedDate);
            }
        });
        
        // Today button click listener
        binding.calendarTodayButton.setOnClickListener(v -> {
            selectedDate = System.currentTimeMillis();
            binding.calendarView.setDate(selectedDate, true, true);
            updateSelectedDateText(selectedDate);
            loadTasksForDate(selectedDate);
        });
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
    
    private void updateMonthYearText(long dateMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM, yyyy", new Locale("vi"));
        String monthYear = sdf.format(new Date(dateMillis));
        // Capitalize first letter
        monthYear = monthYear.substring(0, 1).toUpperCase() + monthYear.substring(1);
        binding.calendarMonthYear.setText(monthYear);
    }
    
    private void updateSelectedDateText(long dateMillis) {
        Calendar today = Calendar.getInstance();
        Calendar selected = Calendar.getInstance();
        selected.setTimeInMillis(dateMillis);
        
        String dateText;
        if (isSameDay(today, selected)) {
            dateText = "Hôm nay";
        } else if (isTomorrow(today, selected)) {
            dateText = "Ngày mai";
        } else if (isYesterday(today, selected)) {
            dateText = "Hôm qua";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM", new Locale("vi"));
            dateText = sdf.format(new Date(dateMillis));
            // Capitalize first letter
            dateText = dateText.substring(0, 1).toUpperCase() + dateText.substring(1);
        }
        
        binding.selectedDateText.setText(dateText);
    }
    
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    private boolean isTomorrow(Calendar today, Calendar selected) {
        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        return isSameDay(tomorrow, selected);
    }
    
    private boolean isYesterday(Calendar today, Calendar selected) {
        Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        return isSameDay(yesterday, selected);
    }
    
    private void loadTasksForDate(long dateMillis) {
        long startOfDay = getStartOfDay(dateMillis);
        long endOfDay = getEndOfDay(dateMillis);
        
        taskViewModel.getTasksByDateRange(startOfDay, endOfDay).observe(getViewLifecycleOwner(), taskEntities -> {
            if (taskEntities != null && !taskEntities.isEmpty()) {
                taskList.clear();
                taskList.addAll(taskEntities);
                adapter.updateTasks(taskList);
                
                // Update task count
                binding.taskCountText.setText(taskEntities.size() + " nhiệm vụ");
                binding.taskCountText.setVisibility(View.VISIBLE);
                
                // Show RecyclerView, hide empty state
                binding.calendarTasksRecyclerview.setVisibility(View.VISIBLE);
                binding.emptyStateLayout.setVisibility(View.GONE);
            } else {
                taskList.clear();
                adapter.updateTasks(taskList);
                
                // Hide task count
                binding.taskCountText.setVisibility(View.GONE);
                
                // Hide RecyclerView, show empty state
                binding.calendarTasksRecyclerview.setVisibility(View.GONE);
                binding.emptyStateLayout.setVisibility(View.VISIBLE);
            }
        });
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
        TaskDetailBottomSheet bottomSheet = new TaskDetailBottomSheet();
        bottomSheet.setTaskEntity(task);
        
        bottomSheet.setOnTaskUpdateListener(updatedTask -> {
            taskViewModel.update(updatedTask);
        });
        
        bottomSheet.setOnTaskDeleteListener(taskToDelete -> {
            taskViewModel.delete(taskToDelete);
        });
        
        bottomSheet.show(getParentFragmentManager(), "TaskDetailBottomSheet");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
