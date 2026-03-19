package hcmute.edu.vn.teeticktick;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import hcmute.edu.vn.teeticktick.database.TaskEntity;
import hcmute.edu.vn.teeticktick.databinding.FragmentTaskDetailBinding;
import hcmute.edu.vn.teeticktick.service.TaskReminderScheduler;
import hcmute.edu.vn.teeticktick.viewmodel.TaskViewModel;

public class TaskDetailFragment extends Fragment {

    private FragmentTaskDetailBinding binding;
    private TaskViewModel taskViewModel;
    private TaskEntity taskEntity;
    
    private int selectedPriority = 1;
    private String selectedCategory = "Personal";
    private String selectedEmoji = "✅";
    private Long selectedDueDate = null;
    private int taskId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        
        // Get task ID from arguments
        if (getArguments() != null) {
            taskId = getArguments().getInt("taskId", -1);
        }
        
        // Load task data
        if (taskId != -1) {
            loadTaskData(taskId);
        }
        
        setupClickListeners();
    }
    
    private void loadTaskData(int taskId) {
        taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), taskEntities -> {
            if (taskEntities != null) {
                for (TaskEntity entity : taskEntities) {
                    if (entity.getId() == taskId) {
                        taskEntity = entity;
                        populateTaskData();
                        break;
                    }
                }
            }
        });
    }
    
    private void populateTaskData() {
        if (taskEntity == null) return;
        
        binding.taskTitleInput.setText(taskEntity.getTitle());
        
        if (taskEntity.getEmoji() != null && !taskEntity.getEmoji().isEmpty()) {
            selectedEmoji = taskEntity.getEmoji();
            binding.emojiSelector.setText(selectedEmoji);
        }
        
        selectedPriority = taskEntity.getPriority();
        selectPriority(
            selectedPriority == 0 ? binding.priorityLow : 
            selectedPriority == 2 ? binding.priorityHigh : binding.priorityMedium, 
            selectedPriority
        );
        
        selectedDueDate = taskEntity.getDueDate();
        if (selectedDueDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            binding.datePick.setText(sdf.format(selectedDueDate));
            selectDate(binding.datePick, selectedDueDate);
        } else {
            selectDate(binding.dateToday, getTodayTimestamp());
        }
        
        selectedCategory = taskEntity.getListName();
        binding.selectCategoryButton.setText(getLocalizedCategoryName(selectedCategory));
        
        if (taskEntity.getDescription() != null) {
            binding.taskDescriptionInput.setText(taskEntity.getDescription());
        }
    }

    private void setupClickListeners() {
        binding.emojiSelector.setOnClickListener(v -> {
            hcmute.edu.vn.teeticktick.bottomsheet.EmojiPickerBottomSheet emojiPicker = 
                new hcmute.edu.vn.teeticktick.bottomsheet.EmojiPickerBottomSheet();
            emojiPicker.setOnEmojiSelectedListener(emoji -> {
                selectedEmoji = emoji;
                binding.emojiSelector.setText(emoji);
            });
            emojiPicker.show(getParentFragmentManager(), "EmojiPicker");
        });
        
        binding.priorityLow.setOnClickListener(v -> selectPriority(binding.priorityLow, 0));
        binding.priorityMedium.setOnClickListener(v -> selectPriority(binding.priorityMedium, 1));
        binding.priorityHigh.setOnClickListener(v -> selectPriority(binding.priorityHigh, 2));
        
        binding.dateToday.setOnClickListener(v -> selectDate(binding.dateToday, getTodayTimestamp()));
        binding.dateTomorrow.setOnClickListener(v -> selectDate(binding.dateTomorrow, getTomorrowTimestamp()));
        binding.dateThisWeek.setOnClickListener(v -> selectDate(binding.dateThisWeek, getThisWeekTimestamp()));
        binding.datePick.setOnClickListener(v -> showDatePicker());
        
        binding.selectCategoryButton.setOnClickListener(v -> {
            hcmute.edu.vn.teeticktick.bottomsheet.ListPickerBottomSheet listPicker = new hcmute.edu.vn.teeticktick.bottomsheet.ListPickerBottomSheet();
            listPicker.setSelectedList(selectedCategory);
            listPicker.setOnListSelectedListener(list -> {
                selectedCategory = list.getKey();
                binding.selectCategoryButton.setText(list.getDisplayName());
            });
            listPicker.show(getParentFragmentManager(), "ListPicker");
        });
        
        binding.updateTaskButton.setOnClickListener(v -> updateTask());
        binding.deleteTaskButton.setOnClickListener(v -> deleteTask());
    }

    private String getLocalizedCategoryName(String categoryKey) {
        if (categoryKey == null) return getString(R.string.list_personal);
        switch (categoryKey) {
            case "Inbox": return getString(R.string.list_inbox);
            case "Work": return getString(R.string.list_work);
            case "Personal": return getString(R.string.list_personal);
            case "Shopping": return getString(R.string.list_shopping);
            case "Learning": return getString(R.string.list_learning);
            default: return categoryKey;
        }
    }

    private void selectPriority(TextView selected, int priority) {
        binding.priorityLow.setSelected(false);
        binding.priorityMedium.setSelected(false);
        binding.priorityHigh.setSelected(false);
        
        selected.setSelected(true);
        selectedPriority = priority;
        
        binding.priorityLow.setTextColor(getResources().getColor(R.color.text_secondary, null));
        binding.priorityMedium.setTextColor(getResources().getColor(R.color.text_secondary, null));
        binding.priorityHigh.setTextColor(getResources().getColor(R.color.text_secondary, null));
        selected.setTextColor(getResources().getColor(R.color.white, null));
    }

    private void selectDate(TextView selected, Long timestamp) {
        binding.dateToday.setSelected(false);
        binding.dateTomorrow.setSelected(false);
        binding.dateThisWeek.setSelected(false);
        binding.datePick.setSelected(false);
        
        selected.setSelected(true);
        selectedDueDate = timestamp;
        
        binding.dateToday.setTextColor(getResources().getColor(R.color.text_secondary, null));
        binding.dateTomorrow.setTextColor(getResources().getColor(R.color.text_secondary, null));
        binding.dateThisWeek.setTextColor(getResources().getColor(R.color.text_secondary, null));
        binding.datePick.setTextColor(getResources().getColor(R.color.text_secondary, null));
        selected.setTextColor(getResources().getColor(R.color.white, null));
    }



    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDueDate != null) {
            calendar.setTimeInMillis(selectedDueDate);
        }
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                selectDate(binding.datePick, selected.getTimeInMillis());
                
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                binding.datePick.setText(sdf.format(selected.getTime()));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateTask() {
        String title = binding.taskTitleInput.getText().toString().trim();
        String description = binding.taskDescriptionInput.getText().toString().trim();
        if (!title.isEmpty() && taskEntity != null) {
            taskEntity.setTitle(title);
            taskEntity.setDescription(description);
            taskEntity.setEmoji(selectedEmoji);
            taskEntity.setPriority(selectedPriority);
            taskEntity.setDueDate(selectedDueDate);
            taskEntity.setListName(selectedCategory);

            taskViewModel.update(taskEntity);

            // Reschedule reminders with new dueDate
            if (getContext() != null) {
                int id = taskEntity.getId();
                TaskReminderScheduler.cancelReminder(getContext(), id);
                TaskReminderScheduler.cancelOverdueReminders(getContext(), id);
                if (selectedDueDate != null && selectedDueDate > System.currentTimeMillis()) {
                    TaskReminderScheduler.scheduleReminder(
                            getContext(), id, title, selectedEmoji, selectedDueDate);
                    TaskReminderScheduler.scheduleOverdueReminders(
                            getContext(), id, title, selectedEmoji, selectedDueDate);
                }
            }

            NavHostFragment.findNavController(TaskDetailFragment.this).navigateUp();
        }
    }

    private void deleteTask() {
        if (taskEntity != null) {
            if (getContext() != null) {
                TaskReminderScheduler.cancelReminder(getContext(), taskEntity.getId());
                TaskReminderScheduler.cancelOverdueReminders(getContext(), taskEntity.getId());
            }
            taskViewModel.delete(taskEntity);
            NavHostFragment.findNavController(TaskDetailFragment.this).navigateUp();
        }
    }

    private long getTodayTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        return calendar.getTimeInMillis();
    }

    private long getTomorrowTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        return calendar.getTimeInMillis();
    }

    private long getThisWeekTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        return calendar.getTimeInMillis();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
