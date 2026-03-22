package hcmute.edu.vn.teeticktick;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import hcmute.edu.vn.teeticktick.bottomsheet.DateTimePickerBottomSheet;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddTaskBottomSheet extends BottomSheetDialogFragment {

    private android.widget.EditText taskTitleInput, taskDescriptionInput;
    private android.widget.ImageView emojiSelector;
    private TextView tabDate, tabDuration;
    private LinearLayout panelDate, panelDuration;
    // Date mode
    private TextView startDateBtn, startTimeBtn;
    // Duration mode
    private TextView durStartDateBtn, durStartTimeBtn, durEndDateBtn, durEndTimeBtn;
    private TextView tvDurationLabel;
    private LinearLayout rowDurStart, rowDurEnd;
    private TextView btnAddHeader, btnCancel;
    private ChipGroup chipGroupAssignees;
    private android.widget.LinearLayout rowExtraInfo;
    private android.widget.LinearLayout extraInfoIconBox;
    private android.widget.ImageView extraInfoIcon;
    private android.widget.TextView tvExtraInfo;

    private int selectedIconRes = R.drawable.ic_ios_check_circle;
    private int selectedIconColor = 0xFF2B7FFF;
    private String selectedCategory = null; // null = chưa chọn
    private Long selectedStartDate = null;
    private Long selectedDueDate = null;
    private boolean isDurationMode = false;
    private boolean categorySelected = false;
    private OnTaskAddedListener listener;

    private final List<String> assignees = new ArrayList<>();
    private static final int REQUEST_CONTACT = 1001;

    public interface OnTaskAddedListener {
        void onTaskAdded(String title, String description, String emoji, String listName,
                         Long startDate, Long dueDate);
    }

    public void setOnTaskAddedListener(OnTaskAddedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupClickListeners();
        expandToFullScreen();

        // Edit mode: pre-fill if editTaskId passed
        Bundle args = getArguments();
        if (args != null && args.containsKey("editTaskId")) {
            prefillEditMode(view, args);
        } else {
            taskTitleInput.requestFocus();
        }
    }

    private void prefillEditMode(View view, Bundle args) {
        // Change header title and button
        TextView headerTitle = view.findViewById(R.id.tv_sheet_title);
        if (headerTitle != null) headerTitle.setText("Chỉnh sửa task");
        btnAddHeader.setText("Lưu");

        String title = args.getString("editTitle", "");
        String desc = args.getString("editDescription", "");
        String emoji = args.getString("editEmoji", "");
        String listName = args.getString("editListName", "");
        long startDate = args.getLong("editStartDate", 0);
        long dueDate = args.getLong("editDueDate", 0);

        taskTitleInput.setText(title);
        taskTitleInput.setSelection(title.length());
        taskDescriptionInput.setText(desc);

        // Parse icon
        if (emoji != null && !emoji.isEmpty()) {
            String iconName = emoji.contains("|") ? emoji.split("\\|")[0] : emoji;
            String colorHex = emoji.contains("|") ? emoji.split("\\|")[1] : null;
            int resId = requireContext().getResources().getIdentifier(
                    iconName, "drawable", requireContext().getPackageName());
            if (resId != 0) selectedIconRes = resId;
            if (colorHex != null) {
                try { selectedIconColor = android.graphics.Color.parseColor(colorHex); } catch (Exception ignored) {}
            }
            updateIconButton();
        }

        // Pre-fill category
        if (listName != null && !listName.isEmpty()) {
            selectedCategory = listName;
            categorySelected = true;
            updateExtraInfoRow(listName, listName, assignees);
        }

        // Pre-fill dates
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (startDate != 0 && dueDate != 0 && startDate != dueDate) {
            // Duration mode
            switchTab(true);
            selectedStartDate = startDate;
            selectedDueDate = dueDate;
            Calendar cs = Calendar.getInstance(); cs.setTimeInMillis(startDate);
            Calendar ce = Calendar.getInstance(); ce.setTimeInMillis(dueDate);
            durStartDateBtn.setText(sdf.format(cs.getTime()));
            durStartTimeBtn.setText(String.format(Locale.getDefault(), "%02d:%02d",
                    cs.get(Calendar.HOUR_OF_DAY), cs.get(Calendar.MINUTE)));
            durEndDateBtn.setText(sdf.format(ce.getTime()));
            durEndTimeBtn.setText(String.format(Locale.getDefault(), "%02d:%02d",
                    ce.get(Calendar.HOUR_OF_DAY), ce.get(Calendar.MINUTE)));
            updateDurationLabel();
        } else if (startDate != 0) {
            selectedStartDate = startDate;
            Calendar cs = Calendar.getInstance(); cs.setTimeInMillis(startDate);
            startDateBtn.setText(sdf.format(cs.getTime()));
            startTimeBtn.setText(String.format(Locale.getDefault(), "%02d:%02d",
                    cs.get(Calendar.HOUR_OF_DAY), cs.get(Calendar.MINUTE)));
        }

        refreshAddButton();
        // Enable save even without re-selecting category
        btnAddHeader.setEnabled(true);
        btnAddHeader.setAlpha(1f);
    }

    private void expandToFullScreen() {
        // Expand bottom sheet to full height when dragged up
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                (com.google.android.material.bottomsheet.BottomSheetDialog) getDialog();
        if (dialog != null) {
            android.widget.FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> behavior =
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight(dpToPx(480));
                behavior.setSkipCollapsed(false);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
                // Allow full expand
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.requestLayout();
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private void initViews(View view) {
        taskTitleInput = view.findViewById(R.id.task_title_input);
        taskDescriptionInput = view.findViewById(R.id.task_description_input);
        emojiSelector = view.findViewById(R.id.emoji_selector);        btnCancel = view.findViewById(R.id.btn_cancel);
        btnAddHeader = view.findViewById(R.id.btn_add_header);

        tabDate = view.findViewById(R.id.tab_date);
        tabDuration = view.findViewById(R.id.tab_duration);
        panelDate = view.findViewById(R.id.panel_date);
        panelDuration = view.findViewById(R.id.panel_duration);

        startDateBtn = view.findViewById(R.id.start_date_btn);
        startTimeBtn = view.findViewById(R.id.start_time_btn);

        durStartDateBtn = view.findViewById(R.id.dur_start_date_btn);
        durStartTimeBtn = view.findViewById(R.id.dur_start_time_btn);
        durEndDateBtn = view.findViewById(R.id.dur_end_date_btn);
        durEndTimeBtn = view.findViewById(R.id.dur_end_time_btn);
        tvDurationLabel = view.findViewById(R.id.tv_duration_label);
        rowDurStart = view.findViewById(R.id.row_dur_start);
        rowDurEnd = view.findViewById(R.id.row_dur_end);

        rowExtraInfo = view.findViewById(R.id.row_extra_info);
        extraInfoIconBox = view.findViewById(R.id.extra_info_icon_box);
        extraInfoIcon = view.findViewById(R.id.extra_info_icon);
        tvExtraInfo = view.findViewById(R.id.tv_extra_info);
        chipGroupAssignees = view.findViewById(R.id.chip_group_assignees);
    }

    private void setupClickListeners() {
        btnCancel.setOnClickListener(v -> dismiss());
        btnAddHeader.setOnClickListener(v -> submitTask());

        emojiSelector.setOnClickListener(v -> {
            hcmute.edu.vn.teeticktick.bottomsheet.CategoryIconPickerBottomSheet picker =
                    new hcmute.edu.vn.teeticktick.bottomsheet.CategoryIconPickerBottomSheet();
            picker.setInitialIcon(selectedIconRes);
            picker.setInitialColor(selectedIconColor);
            picker.setOnIconSelectedListener((iconRes, color) -> {
                selectedIconRes = iconRes;
                selectedIconColor = color;
                updateIconButton();
            });
            picker.show(getParentFragmentManager(), "IconPicker");
        });

        tabDate.setOnClickListener(v -> switchTab(false));
        tabDuration.setOnClickListener(v -> switchTab(true));

        startDateBtn.setOnClickListener(v -> showDatePicker(true, false));
        startTimeBtn.setOnClickListener(v -> showTimePicker(true, false));
        // Whole date row is clickable too
        panelDate.setOnClickListener(v -> showDatePicker(true, false));

        durStartDateBtn.setOnClickListener(v -> showDatePicker(true, true));
        durStartTimeBtn.setOnClickListener(v -> showTimePicker(true, true));
        durEndDateBtn.setOnClickListener(v -> showDatePicker(false, true));
        durEndTimeBtn.setOnClickListener(v -> showTimePicker(false, true));
        rowDurStart.setOnClickListener(v -> showDatePicker(true, true));
        rowDurEnd.setOnClickListener(v -> showDatePicker(false, true));

        rowExtraInfo.setOnClickListener(v -> openExtraInfo());

        // Disable "Thêm" until all 3 conditions met
        btnAddHeader.setAlpha(0.4f);
        btnAddHeader.setEnabled(false);
        taskTitleInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshAddButton();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // Tab Ngày: default to today when bottom sheet opens
        Calendar today = Calendar.getInstance();
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        selectedStartDate = today.getTimeInMillis();
        String todayStr = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(today.getTime());
        startDateBtn.setText(todayStr);
    }

    private void switchTab(boolean toDuration) {
        isDurationMode = toDuration;
        if (toDuration) {
            tabDuration.setBackgroundResource(R.drawable.bg_tab_selected);
            tabDuration.setTextColor(getResources().getColor(android.R.color.black, null));
            tabDate.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tabDate.setTextColor(0xFF4A5565);
            panelDate.setVisibility(View.GONE);
            panelDuration.setVisibility(View.VISIBLE);
            // Set defaults: today 00:00 → 23:59 if not yet chosen
            if (selectedStartDate == null) {
                Calendar start = Calendar.getInstance();
                start.set(Calendar.HOUR_OF_DAY, 0);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                selectedStartDate = start.getTimeInMillis();
                String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(start.getTime());
                durStartDateBtn.setText(dateStr);
                durStartTimeBtn.setText("00:00");
            }
            if (selectedDueDate == null) {
                Calendar end = Calendar.getInstance();
                end.set(Calendar.HOUR_OF_DAY, 23);
                end.set(Calendar.MINUTE, 59);
                end.set(Calendar.SECOND, 0);
                selectedDueDate = end.getTimeInMillis();
                String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(end.getTime());
                durEndDateBtn.setText(dateStr);
                durEndTimeBtn.setText("23:59");
            }
            updateDurationLabel();
        } else {
            tabDate.setBackgroundResource(R.drawable.bg_tab_selected);
            tabDate.setTextColor(getResources().getColor(android.R.color.black, null));
            tabDuration.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tabDuration.setTextColor(0xFF4A5565);
            panelDate.setVisibility(View.VISIBLE);
            panelDuration.setVisibility(View.GONE);
        }
    }

    private void showDatePicker(boolean isStart, boolean isDuration) {
        Long existing = isStart ? selectedStartDate : selectedDueDate;
        long initial = existing != null ? existing : System.currentTimeMillis();

        DateTimePickerBottomSheet picker = new DateTimePickerBottomSheet();
        picker.setInitialMillis(initial);
        picker.setListener(timeMillis -> {
            Calendar picked = Calendar.getInstance();
            picked.setTimeInMillis(timeMillis);
            String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(picked.getTime());
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d",
                    picked.get(Calendar.HOUR_OF_DAY), picked.get(Calendar.MINUTE));
            if (isStart) {
                selectedStartDate = timeMillis;
                (isDuration ? durStartDateBtn : startDateBtn).setText(dateStr);
                (isDuration ? durStartTimeBtn : startTimeBtn).setText(timeStr);
            } else {
                selectedDueDate = timeMillis;
                durEndDateBtn.setText(dateStr);
                durEndTimeBtn.setText(timeStr);
            }
            updateDurationLabel();
        });
        picker.show(getChildFragmentManager(), "date_time_picker");
    }

    private void showTimePicker(boolean isStart, boolean isDuration) {
        // Replaced by DateTimePickerBottomSheet — kept for compatibility
        showDatePicker(isStart, isDuration);
    }

    private void openExtraInfo() {
        hcmute.edu.vn.teeticktick.bottomsheet.ExtraInfoBottomSheet sheet =
                new hcmute.edu.vn.teeticktick.bottomsheet.ExtraInfoBottomSheet();
        sheet.setSelectedCategory(selectedCategory, selectedCategory);
        sheet.setAssignees(assignees);
        sheet.setConfirmListener((categoryKey, categoryName, newAssignees) -> {
            selectedCategory = categoryKey;
            assignees.clear();
            assignees.addAll(newAssignees);
            updateExtraInfoRow(categoryKey, categoryName, newAssignees);
        });
        sheet.show(getChildFragmentManager(), "extra_info");
    }

    private void refreshAddButton() {
        boolean hasTitle = taskTitleInput.getText().toString().trim().length() > 0;
        // Tab Ngày: selectedStartDate is always set (default today), so just check category + title
        // Tab Thời lượng: both start and end are set by default when switching
        boolean hasDate = selectedStartDate != null;
        boolean enabled = hasTitle && categorySelected && hasDate;
        btnAddHeader.setEnabled(enabled);
        btnAddHeader.setAlpha(enabled ? 1f : 0.4f);
    }

    private void updateExtraInfoRow(String categoryKey, String categoryName, List<String> people) {
        // Update icon box: use category icon + color
        int iconRes = hcmute.edu.vn.teeticktick.utils.IconHelper.getIconDrawable(categoryKey);
        int iconColor = hcmute.edu.vn.teeticktick.utils.IconHelper.getIconColor(categoryKey);
        extraInfoIcon.setImageResource(iconRes);
        extraInfoIconBox.getBackground().setTint(iconColor);

        // Update label
        String label = categoryName;
        if (!people.isEmpty()) label += " · " + String.join(", ", people);
        tvExtraInfo.setText(label);

        // Mark category as selected and refresh button
        categorySelected = true;
        selectedCategory = categoryKey;
        refreshAddButton();

        // Sync chips
        chipGroupAssignees.removeAllViews();
        for (String name : people) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
            chip.setText(name);
            chip.setCloseIconVisible(true);
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setChipStrokeWidth(1.5f);
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(0xFF2B7FFF));
            chip.setTextColor(0xFF2B7FFF);
            chip.setOnCloseIconClickListener(v -> {
                assignees.remove(name);
                chipGroupAssignees.removeView(chip);
            });
            chipGroupAssignees.addView(chip);
        }
    }

    private void updateDurationLabel() {
        if (selectedStartDate != null && selectedDueDate != null && tvDurationLabel != null) {
            long diffMs = selectedDueDate - selectedStartDate;
            long diffDays = diffMs / (1000 * 60 * 60 * 24);
            long diffHours = (diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
            String label;
            if (diffDays > 0) label = "Thời lượng: " + diffDays + " ngày";
            else if (diffHours > 0) label = "Thời lượng: " + diffHours + " giờ";
            else label = "Thời lượng: < 1 giờ";
            tvDurationLabel.setText(label);
            tvDurationLabel.setVisibility(View.VISIBLE);
        }
    }

    private void pickContact() {
        if (requireContext().checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, REQUEST_CONTACT);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CONTACT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CONTACT
                && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pickContact();
        } else {
            Toast.makeText(requireContext(), "Cần quyền truy cập danh bạ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONTACT && resultCode == Activity.RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
            try (Cursor cursor = requireContext().getContentResolver()
                    .query(contactUri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    addAssigneeChip(cursor.getString(0));
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Không thể đọc danh bạ", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addAssigneeChip(String name) {
        if (assignees.contains(name)) return;
        assignees.add(name);

        // Update "Thông tin thêm" label
        TextView tvExtraInfo = getView() != null ? getView().findViewById(R.id.tv_extra_info) : null;
        if (tvExtraInfo != null) {
            tvExtraInfo.setText(String.join(", ", assignees));
        }

        Chip chip = new Chip(requireContext());
        chip.setText(name);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(android.R.color.white);
        chip.setChipStrokeWidth(1.5f);
        chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(0xFF2B7FFF));
        chip.setTextColor(0xFF2B7FFF);
        chip.setOnCloseIconClickListener(v -> {
            assignees.remove(name);
            chipGroupAssignees.removeView(chip);
            if (tvExtraInfo != null) {
                tvExtraInfo.setText(assignees.isEmpty() ? "Chưa thiết lập" : String.join(", ", assignees));
            }
        });
        chipGroupAssignees.addView(chip);
    }

    private void updateIconButton() {
        emojiSelector.setImageResource(selectedIconRes);
        emojiSelector.setColorFilter(android.graphics.Color.WHITE);
        emojiSelector.getBackground().setTint(selectedIconColor);
    }

    private void submitTask() {
        String title = taskTitleInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập tên nhiệm vụ", Toast.LENGTH_SHORT).show();
            taskTitleInput.requestFocus();
            return;
        }
        if (!categorySelected) {
            Toast.makeText(getContext(), "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }
        String description = taskDescriptionInput.getText().toString().trim();
        String iconName = getResources().getResourceEntryName(selectedIconRes);
        // Encode icon color into emoji field: "iconName|#RRGGBB"
        String iconColor = String.format("#%06X", (0xFFFFFF & selectedIconColor));
        String emojiField = iconName + "|" + iconColor;

        // Tab Ngày: dueDate = selectedStartDate (single day task)
        // Tab Thời lượng: startDate + dueDate already set
        Long startDate = selectedStartDate;
        Long dueDate = isDurationMode ? selectedDueDate : selectedStartDate;

        if (listener != null) {
            listener.onTaskAdded(title, description, emojiField, selectedCategory, startDate, dueDate);
            dismiss();
        }
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        // Don't restore toolbar if called from TaskDetailFragment
        Bundle args = getArguments();
        if (args != null && args.getBoolean("calledFromDetail", false)) {
            return;
        }
        // Only restore toolbar if NOT inside TaskDetailFragment
        if (getActivity() instanceof MainActivity) {
            androidx.navigation.NavController nav = null;
            try {
                nav = androidx.navigation.Navigation.findNavController(
                        getActivity(), R.id.nav_host_fragment_content_main);
            } catch (Exception ignored) {}
            if (nav != null && nav.getCurrentDestination() != null
                    && nav.getCurrentDestination().getId() == R.id.TaskDetailFragment) {
                return; // Don't restore — TaskDetailFragment manages its own toolbar state
            }
            ((MainActivity) getActivity()).restoreToolbarAndFab();
        }
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}
