package hcmute.edu.vn.teeticktick;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import hcmute.edu.vn.teeticktick.database.TaskEntity;

public class TaskDetailBottomSheet extends BottomSheetDialogFragment {

    private EditText taskTitleInput;
    private TextView emojiSelector;
    private TextView priorityLow, priorityMedium, priorityHigh;
    private TextView dateToday, dateTomorrow, dateThisWeek, datePick;
    private TextView categoryPersonal, categoryWork, categoryShopping;
    private Button updateTaskButton, deleteTaskButton;
    private ImageView closeButton;
    
    private int selectedPriority = 1;
    private String selectedCategory = "Personal";
    private String selectedEmoji = "✅";
    private Long selectedDueDate = null;
    
    private TaskEntity taskEntity;
    private OnTaskUpdateListener updateListener;
    private OnTaskDeleteListener deleteListener;

    public interface OnTaskUpdateListener {
        void onTaskUpdate(TaskEntity task);
    }
    
    public interface OnTaskDeleteListener {
        void onTaskDelete(TaskEntity task);
    }

    public void setTaskEntity(TaskEntity task) {
        this.taskEntity = task;
    }

    public void setOnTaskUpdateListener(OnTaskUpdateListener listener) {
        this.updateListener = listener;
    }
    
    public void setOnTaskDeleteListener(OnTaskDeleteListener listener) {
        this.deleteListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_task_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        
        if (taskEntity != null) {
            populateTaskData();
        }
        
        setupClickListeners();
    }

    private void initViews(View view) {
        taskTitleInput = view.findViewById(R.id.task_title_input);
        emojiSelector = view.findViewById(R.id.emoji_selector);
        closeButton = view.findViewById(R.id.close_button);
        
        priorityLow = view.findViewById(R.id.priority_low);
        priorityMedium = view.findViewById(R.id.priority_medium);
        priorityHigh = view.findViewById(R.id.priority_high);
        
        dateToday = view.findViewById(R.id.date_today);
        dateTomorrow = view.findViewById(R.id.date_tomorrow);
        dateThisWeek = view.findViewById(R.id.date_this_week);
        datePick = view.findViewById(R.id.date_pick);
        
        categoryPersonal = view.findViewById(R.id.category_personal);
        categoryWork = view.findViewById(R.id.category_work);
        categoryShopping = view.findViewById(R.id.category_shopping);
        
        updateTaskButton = view.findViewById(R.id.update_task_button);
        deleteTaskButton = view.findViewById(R.id.delete_task_button);
    }
    
    private void populateTaskData() {
        taskTitleInput.setText(taskEntity.getTitle());
        
        if (taskEntity.getEmoji() != null && !taskEntity.getEmoji().isEmpty()) {
            selectedEmoji = taskEntity.getEmoji();
            emojiSelector.setText(selectedEmoji);
        }
        
        selectedPriority = taskEntity.getPriority();
        selectPriority(
            selectedPriority == 0 ? priorityLow : 
            selectedPriority == 2 ? priorityHigh : priorityMedium, 
            selectedPriority
        );
        
        selectedDueDate = taskEntity.getDueDate();
        if (selectedDueDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            datePick.setText(sdf.format(selectedDueDate));
            selectDate(datePick, selectedDueDate);
        } else {
            selectDate(dateToday, getTodayTimestamp());
        }
        
        selectedCategory = taskEntity.getListName();
        TextView categoryView = categoryPersonal;
        if ("Work".equals(selectedCategory)) {
            categoryView = categoryWork;
        } else if ("Shopping".equals(selectedCategory)) {
            categoryView = categoryShopping;
        }
        selectCategory(categoryView, selectedCategory);
    }

    private void setupClickListeners() {
        closeButton.setOnClickListener(v -> dismiss());
        
        emojiSelector.setOnClickListener(v -> {
            hcmute.edu.vn.teeticktick.bottomsheet.EmojiPickerBottomSheet emojiPicker = 
                new hcmute.edu.vn.teeticktick.bottomsheet.EmojiPickerBottomSheet();
            emojiPicker.setOnEmojiSelectedListener(emoji -> {
                selectedEmoji = emoji;
                emojiSelector.setText(emoji);
            });
            emojiPicker.show(getParentFragmentManager(), "EmojiPicker");
        });
        
        priorityLow.setOnClickListener(v -> selectPriority(priorityLow, 0));
        priorityMedium.setOnClickListener(v -> selectPriority(priorityMedium, 1));
        priorityHigh.setOnClickListener(v -> selectPriority(priorityHigh, 2));
        
        dateToday.setOnClickListener(v -> selectDate(dateToday, getTodayTimestamp()));
        dateTomorrow.setOnClickListener(v -> selectDate(dateTomorrow, getTomorrowTimestamp()));
        dateThisWeek.setOnClickListener(v -> selectDate(dateThisWeek, getThisWeekTimestamp()));
        datePick.setOnClickListener(v -> showDatePicker());
        
        categoryPersonal.setOnClickListener(v -> selectCategory(categoryPersonal, "Personal"));
        categoryWork.setOnClickListener(v -> selectCategory(categoryWork, "Work"));
        categoryShopping.setOnClickListener(v -> selectCategory(categoryShopping, "Shopping"));
        
        updateTaskButton.setOnClickListener(v -> {
            String title = taskTitleInput.getText().toString().trim();
            if (!title.isEmpty() && taskEntity != null && updateListener != null) {
                taskEntity.setTitle(title);
                taskEntity.setEmoji(selectedEmoji);
                taskEntity.setPriority(selectedPriority);
                taskEntity.setDueDate(selectedDueDate);
                taskEntity.setListName(selectedCategory);
                
                updateListener.onTaskUpdate(taskEntity);
                dismiss();
            }
        });
        
        deleteTaskButton.setOnClickListener(v -> {
            if (taskEntity != null && deleteListener != null) {
                deleteListener.onTaskDelete(taskEntity);
                dismiss();
            }
        });
    }

    private void selectPriority(TextView selected, int priority) {
        priorityLow.setSelected(false);
        priorityMedium.setSelected(false);
        priorityHigh.setSelected(false);
        
        selected.setSelected(true);
        selectedPriority = priority;
        
        priorityLow.setTextColor(getResources().getColor(R.color.text_secondary, null));
        priorityMedium.setTextColor(getResources().getColor(R.color.text_secondary, null));
        priorityHigh.setTextColor(getResources().getColor(R.color.text_secondary, null));
        selected.setTextColor(getResources().getColor(R.color.white, null));
    }

    private void selectDate(TextView selected, Long timestamp) {
        dateToday.setSelected(false);
        dateTomorrow.setSelected(false);
        dateThisWeek.setSelected(false);
        datePick.setSelected(false);
        
        selected.setSelected(true);
        selectedDueDate = timestamp;
        
        dateToday.setTextColor(getResources().getColor(R.color.text_secondary, null));
        dateTomorrow.setTextColor(getResources().getColor(R.color.text_secondary, null));
        dateThisWeek.setTextColor(getResources().getColor(R.color.text_secondary, null));
        datePick.setTextColor(getResources().getColor(R.color.text_secondary, null));
        selected.setTextColor(getResources().getColor(R.color.white, null));
    }

    private void selectCategory(TextView selected, String category) {
        categoryPersonal.setSelected(false);
        categoryWork.setSelected(false);
        categoryShopping.setSelected(false);
        
        selected.setSelected(true);
        selectedCategory = category;
        
        categoryPersonal.setTextColor(getResources().getColor(R.color.text_secondary, null));
        categoryWork.setTextColor(getResources().getColor(R.color.text_secondary, null));
        categoryShopping.setTextColor(getResources().getColor(R.color.text_secondary, null));
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
                selectDate(datePick, selected.getTimeInMillis());
                
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                datePick.setText(sdf.format(selected.getTime()));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
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
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}
