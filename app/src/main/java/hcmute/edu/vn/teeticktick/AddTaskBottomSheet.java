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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTaskBottomSheet extends BottomSheetDialogFragment {

    private EditText taskTitleInput;
    private EditText taskDescriptionInput;
    private TextView emojiSelector;
    private TextView priorityLow, priorityMedium, priorityHigh;
    private TextView dateToday, dateTomorrow, dateThisWeek, datePick;
    private TextView startDateBtn, startTimeBtn, endDateBtn, endTimeBtn;
    private TextView startToday, startTomorrow, startPick;
    private TextView endToday, endTomorrow, endThisWeek, endPick;
    private TextView selectCategoryButton;
    private Button addTaskButton;
    private ImageView closeButton;
    
    private int selectedPriority = 1;
    private String selectedCategory = "Personal";
    private String selectedEmoji = "✅";
    private Long selectedStartDate = null;
    private Long selectedDueDate = null;
    private OnTaskAddedListener listener;

    public interface OnTaskAddedListener {
        void onTaskAdded(String title, String description, String emoji, String listName, Long startDate, Long dueDate);
    }

    public void setOnTaskAddedListener(OnTaskAddedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupClickListeners();
        
        // Set default selections
        selectPriority(priorityMedium, 1);
        selectedCategory = "Personal";
        selectCategoryButton.setText(getString(R.string.list_personal));
        
        taskTitleInput.requestFocus();
    }

    private void initViews(View view) {
        taskTitleInput = view.findViewById(R.id.task_title_input);
        taskDescriptionInput = view.findViewById(R.id.task_description_input);
        emojiSelector = view.findViewById(R.id.emoji_selector);
        closeButton = view.findViewById(R.id.close_button);

        priorityLow = view.findViewById(R.id.priority_low);
        priorityMedium = view.findViewById(R.id.priority_medium);
        priorityHigh = view.findViewById(R.id.priority_high);

        startDateBtn = view.findViewById(R.id.start_date_btn);
        startTimeBtn = view.findViewById(R.id.start_time_btn);
        endDateBtn   = view.findViewById(R.id.end_date_btn);
        endTimeBtn   = view.findViewById(R.id.end_time_btn);

        startToday    = view.findViewById(R.id.start_today);
        startTomorrow = view.findViewById(R.id.start_tomorrow);
        startPick     = view.findViewById(R.id.start_pick);
        endToday      = view.findViewById(R.id.end_today);
        endTomorrow   = view.findViewById(R.id.end_tomorrow);
        endThisWeek   = view.findViewById(R.id.end_this_week);
        endPick       = view.findViewById(R.id.end_pick);

        selectCategoryButton = view.findViewById(R.id.select_category_button);
        addTaskButton = view.findViewById(R.id.add_task_button);
    }

    private void setupClickListeners() {
        closeButton.setOnClickListener(v -> dismiss());

        emojiSelector.setOnClickListener(v -> {
            hcmute.edu.vn.teeticktick.bottomsheet.EmojiPickerBottomSheet picker =
                    new hcmute.edu.vn.teeticktick.bottomsheet.EmojiPickerBottomSheet();
            picker.setOnEmojiSelectedListener(emoji -> {
                selectedEmoji = emoji;
                emojiSelector.setText(emoji);
            });
            picker.show(getParentFragmentManager(), "EmojiPicker");
        });

        priorityLow.setOnClickListener(v -> selectPriority(priorityLow, 0));
        priorityMedium.setOnClickListener(v -> selectPriority(priorityMedium, 1));
        priorityHigh.setOnClickListener(v -> selectPriority(priorityHigh, 2));

        startDateBtn.setOnClickListener(v -> showDatePickerFor(true));
        startTimeBtn.setOnClickListener(v -> showTimePickerFor(true));
        endDateBtn.setOnClickListener(v -> showDatePickerFor(false));
        endTimeBtn.setOnClickListener(v -> showTimePickerFor(false));

        // Quick chips — start
        startToday.setOnClickListener(v -> applyQuickDate(true, 0));
        startTomorrow.setOnClickListener(v -> applyQuickDate(true, 1));
        startPick.setOnClickListener(v -> showDatePickerFor(true));

        // Quick chips — end
        endToday.setOnClickListener(v -> applyQuickDate(false, 0));
        endTomorrow.setOnClickListener(v -> applyQuickDate(false, 1));
        endThisWeek.setOnClickListener(v -> applyQuickDate(false, 7));
        endPick.setOnClickListener(v -> showDatePickerFor(false));

        selectCategoryButton.setOnClickListener(v -> {
            hcmute.edu.vn.teeticktick.bottomsheet.ListPickerBottomSheet listPicker =
                    new hcmute.edu.vn.teeticktick.bottomsheet.ListPickerBottomSheet();
            listPicker.setSelectedList(selectedCategory);
            listPicker.setOnListSelectedListener(list -> {
                selectedCategory = list.getKey();
                selectCategoryButton.setText(list.getDisplayName());
            });
            listPicker.show(getParentFragmentManager(), "ListPicker");
        });

        addTaskButton.setOnClickListener(v -> {
            String title = taskTitleInput.getText().toString().trim();
            String description = taskDescriptionInput.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) {
                listener.onTaskAdded(title, description, selectedEmoji, selectedCategory,
                        selectedStartDate, selectedDueDate);
                dismiss();
            }
        });
    }

    /** daysOffset: 0=today, 1=tomorrow, 7=this week end */
    private void applyQuickDate(boolean isStart, int daysOffset) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, daysOffset);
        // Keep existing time if already set, otherwise default to 08:00 (start) or 23:59 (end)
        Long existing = isStart ? selectedStartDate : selectedDueDate;
        if (existing != null) {
            Calendar existingCal = Calendar.getInstance();
            existingCal.setTimeInMillis(existing);
            cal.set(Calendar.HOUR_OF_DAY, existingCal.get(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, existingCal.get(Calendar.MINUTE));
        } else {
            cal.set(Calendar.HOUR_OF_DAY, isStart ? 8 : 23);
            cal.set(Calendar.MINUTE, isStart ? 0 : 59);
        }
        cal.set(Calendar.SECOND, 0);
        String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime());
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d",
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        if (isStart) {
            selectedStartDate = cal.getTimeInMillis();
            startDateBtn.setText(dateStr);
            startTimeBtn.setText(timeStr);
            highlightChip(startToday, startTomorrow, startPick, daysOffset);
        } else {
            selectedDueDate = cal.getTimeInMillis();
            endDateBtn.setText(dateStr);
            endTimeBtn.setText(timeStr);
            highlightChip(endToday, endTomorrow, endThisWeek, endPick, daysOffset);
        }
        // Auto open time picker after quick date selection
        showTimePickerFor(isStart);
    }

    private void highlightChip(TextView today, TextView tomorrow, TextView pick, int daysOffset) {
        resetChip(today); resetChip(tomorrow); resetChip(pick);
        if (daysOffset == 0) selectChip(today);
        else if (daysOffset == 1) selectChip(tomorrow);
        else selectChip(pick);
    }

    private void highlightChip(TextView today, TextView tomorrow, TextView thisWeek, TextView pick, int daysOffset) {
        resetChip(today); resetChip(tomorrow); resetChip(thisWeek); resetChip(pick);
        if (daysOffset == 0) selectChip(today);
        else if (daysOffset == 1) selectChip(tomorrow);
        else if (daysOffset == 7) selectChip(thisWeek);
        else selectChip(pick);
    }

    private void selectChip(TextView chip) {
        chip.setSelected(true);
        chip.setTextColor(getResources().getColor(R.color.white, null));
    }

    private void resetChip(TextView chip) {
        chip.setSelected(false);
        chip.setTextColor(getResources().getColor(R.color.text_secondary, null));
    }

    private void showDatePickerFor(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        Long existing = isStart ? selectedStartDate : selectedDueDate;
        if (existing != null) cal.setTimeInMillis(existing);

        new DatePickerDialog(requireContext(), R.style.CustomPickerDialogTheme,
                (view, year, month, day) -> {
                    Calendar picked = Calendar.getInstance();
                    if (existing != null) picked.setTimeInMillis(existing);
                    picked.set(year, month, day);
                    String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(picked.getTime());
                    if (isStart) {
                        selectedStartDate = picked.getTimeInMillis();
                        startDateBtn.setText(dateStr);
                        highlightChip(startToday, startTomorrow, startPick, -1); // custom → highlight pick
                    } else {
                        selectedDueDate = picked.getTimeInMillis();
                        endDateBtn.setText(dateStr);
                        highlightChip(endToday, endTomorrow, endThisWeek, endPick, -1);
                    }
                    showTimePickerFor(isStart);
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePickerFor(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        Long existing = isStart ? selectedStartDate : selectedDueDate;
        if (existing != null) cal.setTimeInMillis(existing);

        new android.app.TimePickerDialog(requireContext(), R.style.CustomPickerDialogTheme,
                (view, hour, minute) -> {
                    Calendar current = Calendar.getInstance();
                    if (existing != null) current.setTimeInMillis(existing);
                    current.set(Calendar.HOUR_OF_DAY, hour);
                    current.set(Calendar.MINUTE, minute);
                    current.set(Calendar.SECOND, 0);
                    String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    if (isStart) {
                        selectedStartDate = current.getTimeInMillis();
                        startTimeBtn.setText(timeStr);
                    } else {
                        selectedDueDate = current.getTimeInMillis();
                        endTimeBtn.setText(timeStr);
                    }
                },
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
        ).show();
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

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        // Restore toolbar and FAB in MainActivity
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).restoreToolbarAndFab();
        }
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}
